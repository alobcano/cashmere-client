package com.hbr.cashmere.client.service;

import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import com.hbr.cashmere.client.model.CsvRow;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CsvService {
  private final S3FileService s3FileService;

  public CsvService(S3FileService s3FileService) {
    this.s3FileService = s3FileService;
  }

  public void processCsv(List<CsvRow> rows) {
    for (CsvRow row : rows) {
      String s3Path = row.getS3Path();
      String[] parts = s3Path.replace("s3://", "").split("/", 2);
      if (parts.length == 2) {
        String bucketName = parts[0];
        String key = parts[1];
        String filename = key.substring(key.lastIndexOf('/') + 1);
        try {
          log.info("processing file: {}", filename);
          Path destinationPath = Path.of(filename); // Adjust as needed
          s3FileService.downloadAndCleanXmlFile(bucketName, key, destinationPath); // Provide destination path as needed
        } catch (Exception e) {
              log.error("Error processing file: {}", filename, e);
        }
      }
    }
  }
}
