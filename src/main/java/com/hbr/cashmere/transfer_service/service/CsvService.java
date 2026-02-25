package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.OmnipubDTO;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CsvService {

  private final S3FileService s3FileService;

  public CsvService(S3FileService s3FileService) {
    this.s3FileService = s3FileService;
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
          OmnipubDTO dto = createOmnipubDTO(row, xmlFile, collectionId);
        } catch (Exception e) {
          log.error("Error processing file: {}", filename, e);
        }
      }
    }
  }

  private OmnipubDTO createOmnipubDTO(
    CsvRow row,
    byte[] fileContent,
    int collectionId
  ) {
    OmnipubDTO dto = new OmnipubDTO();
    dto.setCollectionIds(new int[] { collectionId });
    dto.setExternalId(row.getAvailabilityPk());

    OmnipubMetadata metadata = new OmnipubMetadata();
    metadata.setTitle(XmlUtil.extractTitle(fileContent));
    metadata.setAuthors(XmlUtil.extractAuthors(fileContent));
    metadata.setPublisher("Harvard Business School Publishing - HBD");
    metadata.setPublicationDate(XmlUtil.extractPublishedDate(fileContent));

    dto.setMetadata(metadata);
    dto.setFile(fileContent);
    return dto;
  }
}
