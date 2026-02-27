package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CsvService {

  private final S3FileService s3FileService;
  private final CashmereService cashmereService;

  public CsvService(
    S3FileService s3FileService,
    CashmereService cashmereService
  ) {
    this.s3FileService = s3FileService;
    this.cashmereService = cashmereService;
  }

  public void processCsv(List<CsvRow> rows, int collectionId) {
    for (CsvRow row : rows) {
      String s3Path = row.getS3Path();
      String[] parts = s3Path.replace("s3://", "").split("/", 2);
      if (parts.length == 2) {
        String bucketName = parts[0];
        String key = parts[1];
        String filename = key.substring(key.lastIndexOf('/') + 1);
        byte[] xmlFile;
        try {
          log.info("processing file: {}", filename);
          xmlFile = s3FileService.downloadFile(bucketName, key);
          this.createOmnipub(
            this.getMetadata(xmlFile),
            xmlFile,
            collectionId,
            row,
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
    CsvRow row,
    String filename
  ) {
    try {
      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("collection_ids", collectionId);
      builder.part("external_id", row.getAvailabilityPk());

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
              row.getAvailabilityPk()
            ),
          error ->
            log.error(
              "Error creating Omnipub in Cashmere for title: {} and external_id: {}.",
              metadata.getTitle(),
              row.getAvailabilityPk(),
              error
            )
        );
    } catch (Exception e) {
      log.error("Error creating Omnipub for title: {}", metadata.getTitle(), e);
    }
  }
}
