package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.constants.CsvConstants;
import com.hbr.cashmere.transfer_service.constants.XmlConstants;
import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.model.GitHubFileWithMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

  /**
   * Processes a list of CSV rows, downloading XML files from S3, extracting metadata, and creating Omnipubs in Cashmere.
   *
   * @param rows The list of CSV rows to process
   * @param collectionId The collection ID to associate with the created Omnipubs
   */
  public void processCsv(List<CsvSnowflakeRow> rows, int collectionId) {
    for (CsvSnowflakeRow row : rows) {
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

  /**
   * Extracts metadata from the XML file content to create an OmnipubMetadata object.
   *
   * @param fileContent The XML file content
   * @return An OmnipubMetadata object containing the extracted metadata
   */
  private OmnipubMetadata getMetadata(byte[] fileContent) {
    OmnipubMetadata metadata = new OmnipubMetadata();
    metadata.setTitle(XmlUtil.extractTitle(fileContent));
    metadata.setAuthors(XmlUtil.extractAuthors(fileContent));
    metadata.setPublisher("Harvard Business School Publishing - HBD");
    metadata.setPublicationDate(
      XmlUtil.extractDate(fileContent, XmlConstants.PUBLISHED_TAG)
    );
    metadata.setLastUpdatedDate(
      XmlUtil.extractDate(fileContent, XmlConstants.UPDATED_TAG)
    );

    return metadata;
  }

  /**
   * Creates an Omnipub in Cashmere using the provided metadata and file content.
   * @param metadata The metadata for the Omnipub
   * @param fileContent The XML file content
   * @param collectionId The collection ID to associate with the Omnipub
   * @param externalId The external ID for the Omnipub
   * @param filename The filename for the XML file
   */
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
        "creation_date",
        metadata.getPublicationDate(),
        "updated_date",
        metadata.getLastUpdatedDate()
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

  /**
   * Processes a list of video CSV rows, downloading XML files from Git, extracting metadata, and creating Omnipubs in Cashmere.
   *
   * @param rows The list of video CSV rows to process
   * @param collectionId The collection ID to associate with the Omnipubs
   */
  public void processVideoCsv(List<CsvVideoRow> rows, int collectionId) {
    for (CsvVideoRow row : rows) {
      log.info(
        "Processing video row with title: {} and external_id: {}",
        row.getTitle(),
        row.getAvailabilityPk()
      );
      String filename = String.format("%s.xml", row.getAlternateIdValue1());
      try {
        log.info("processing file: {}", filename);

        GitHubFileWithMetadata fileWithMetadata = gitService
          .downloadXmlWithMetadata(filename)
          .block();
        byte[] xmlFile = fileWithMetadata.getContent();

        contentService
          .fetchMetadata(row.getAvailabilityPk())
          .subscribe(json -> {
            JsonNode availability = json.get("availabilities").get(0);
            String author = availability.get("author").asString();
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(
              "yyyy-MM-dd HH:mm:ss.SSS"
            );
            String publicationDate = LocalDateTime.parse(
              availability.get("publicationDate").asString(),
              inputFormatter
            )
              .atOffset(ZoneOffset.UTC)
              .format(DateTimeFormatter.ISO_INSTANT);
            String[] authors = List.of(author).toArray(new String[0]);
            OmnipubMetadata metadata = new OmnipubMetadata(
              row.getTitle(),
              authors,
              "Harvard Business School Publishing - Corporate Learning",
              publicationDate,
              fileWithMetadata
                .getLastCommitDate()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)
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

  public Map<String, String> getCashmereId(String externalId) {
    JsonNode response = cashmereService.getOmnipubs(externalId).block();
    if (
      response != null &&
      response.has(CsvConstants.ITEMS) &&
      response.get(CsvConstants.ITEMS).isArray() &&
      response.get(CsvConstants.ITEMS).size() > 0
    ) {
      JsonNode firstItem = response.get(CsvConstants.ITEMS).get(0);
      String cashmereId = firstItem.get("uuid").asString();
      return Map.of("cashmereId", cashmereId, "externalId", externalId);
    }
    return Map.of("cashmereId", "Not found", "externalId", externalId);
  }

  public void deleteOmnipubs(List<CsvDeletionManifestRow> rows) {
    for (CsvDeletionManifestRow row : rows) {
      log.info(
        "Processing deletion manifest row with coreProductId: {} and availabilityPk: {}",
        row.getCoreProductId(),
        row.getAvailabilityPk()
      );
      try {
        JsonNode response = cashmereService
          .getOmnipubs(row.getAvailabilityPk())
          .block();
        if (
          response != null &&
          response.has(CsvConstants.ITEMS) &&
          response.get(CsvConstants.ITEMS).isArray() &&
          response.get(CsvConstants.ITEMS).size() > 0
        ) {
          JsonNode firstItem = response.get(CsvConstants.ITEMS).get(0);
          String cashmereUuid = firstItem.get("uuid").asString();
          cashmereService
            .deleteOmnipub(cashmereUuid)
            .subscribe(
              successResponse ->
                log.info(
                  "Successfully sent delete request for Cashmere UUID: {}, coreProductId: {}, availabilityPk: {}. Response: {}",
                  cashmereUuid,
                  row.getCoreProductId(),
                  row.getAvailabilityPk(),
                  successResponse
                ),
              error ->
                log.error(
                  "Error sending delete request for Cashmere UUID: {}, coreProductId: {}, availabilityPk: {}",
                  cashmereUuid,
                  row.getCoreProductId(),
                  row.getAvailabilityPk(),
                  error
                )
            );
        } else {
          log.warn(
            "No Omnipub found in Cashmere for availabilityPk: {}. Skipping deletion.",
            row.getAvailabilityPk()
          );
        }
      } catch (Exception e) {
        log.error(
          "Error processing deletion manifest row with coreProductId: {} and availabilityPk: {}",
          row.getCoreProductId(),
          row.getAvailabilityPk(),
          e
        );
      }
    }
  }
}
