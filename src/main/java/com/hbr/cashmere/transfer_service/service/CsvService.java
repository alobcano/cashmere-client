package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
public class CsvService {

  private final S3FileService s3FileService;
  private final CashmereService cashmereService;
  private final ContentService contentService;
  private final GitService gitService;

  public CsvService(
    S3FileService s3FileService,
    CashmereService cashmereService,
    ContentService contentService,
    GitService gitService
  ) {
    this.s3FileService = s3FileService;
    this.cashmereService = cashmereService;
    this.contentService = contentService;
    this.gitService = gitService;
  }

  public void processCsv(List<CsvRow> rows, int collectionId) {
    for (CsvRow row : rows) {
      String s3Path = row.getS3Path();
      String[] parts = s3Path.replace("s3://", "").split("/", 2);
      if (parts.length == 2) {
        String bucketName = parts[0];
        String key = parts[1];
        String filename = key.substring(key.lastIndexOf('/') + 1);
        key = key.replace("article-content", "podcast-content"); // Temporary fix for podcast content
        byte[] xmlFile;
        try {
          log.info("processing file: {}", filename);
          xmlFile = s3FileService.downloadFile(bucketName, key);
          this.createOmnipub(
            this.getMetadata(xmlFile),
            xmlFile,
            collectionId,
            row.getAvailabilityPk(),
            filename
          );
        } catch (Exception e) {
          log.error("Error processing file: {}", filename, e);
        }
      }
    }
  }

  private OmnipubMetadata getMetadata(byte[] fileContent) {
    OmnipubMetadata metadata = new OmnipubMetadata();
    metadata.setTitle(XmlUtil.extractTitle(fileContent));
    metadata.setAuthors(XmlUtil.extractAuthors(fileContent));
    metadata.setPublisher("Harvard Business School Publishing - HBD");
    metadata.setPublicationDate(XmlUtil.extractPublishedDate(fileContent));

    return metadata;
  }

  private void createOmnipub(
    OmnipubMetadata metadata,
    byte[] fileContent,
    int collectionId,
    String externalId,
    String filename
  ) {
    try {
      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("collection_ids", collectionId);
      builder.part("external_id", externalId);

      Map<String, Object> metadataMap = Map.of(
        "title",
        metadata.getTitle(),
        "authors",
        metadata.getAuthors(),
        "publisher",
        metadata.getPublisher(),
        "publication_date",
        metadata.getPublicationDate().toString()
      );
      builder.part("metadata", metadataMap, MediaType.APPLICATION_JSON);

      Resource resource = new ByteArrayResource(fileContent) {
        @Override
        public String getFilename() {
          return filename;
        }
      };
      builder.part("file", resource, MediaType.APPLICATION_XML);

      cashmereService
        .createOmnipub(builder.build())
        .subscribe(
          response ->
            log.info(
              "Successfully created Omnipub in Cashmere for title: {} and external_id: {}.",
              metadata.getTitle(),
              externalId
            ),
          error ->
            log.error(
              "Error creating Omnipub in Cashmere for title: {} and external_id: {}.",
              metadata.getTitle(),
              externalId,
              error
            )
        );
    } catch (Exception e) {
      log.error("Error creating Omnipub for title: {}", metadata.getTitle(), e);
    }
  }

  public void processVideoCsv(List<CsvVideoRow> rows, int collectionId) {
    for (CsvVideoRow row : rows) {
      log.info(
        "Processing video row with title: {} and external_id: {}",
        row.getTitle(),
        row.getAvailabilityPk()
      );
      String filename = String.format("%s.xml", row.getAlternateIdValue1());
      byte[] xmlFile;
      try {
        log.info("processing file: {}", filename);
        xmlFile = gitService.downloadXml(filename).block();

        contentService
          .fetchMetadata(row.getAvailabilityPk())
          .subscribe(json -> {
            JsonNode availability = json.get("availabilities").get(0);
            String author = availability.get("author").asString();
            String publicationDate = availability
              .get("publicationDate")
              .asString();
            String[] authors = List.of(author).toArray(new String[0]);
            OmnipubMetadata metadata = new OmnipubMetadata(
              row.getTitle(),
              authors,
              "Harvard Business School Publishing - Corporate Learning",
              LocalDateTime.parse(
                publicationDate,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
              )
            );
            this.createOmnipub(
              metadata,
              xmlFile,
              collectionId,
              row.getAvailabilityPk(),
              filename
            );
          });
      } catch (Exception e) {
        log.error("Error processing file: {}", filename, e);
      }
    }
  }
}
