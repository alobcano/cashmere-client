package com.hbr.cashmere.transfer_service.controller;

import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.service.CsvService;
import com.hbr.cashmere.transfer_service.util.CsvUtil;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/csv")
public class CsvUploadController {

  private final CsvService csvService;

  public CsvUploadController(CsvService csvService) {
    this.csvService = csvService;
  }

  /**
   * Endpoint to upload a CSV file and process its content. The CSV is expected to contain rows of data that will be processed by the CsvService.
   * @param file The CSV file to upload
   * @param collectionId The collection ID to associate with the processed rows
   * @return A ResponseEntity indicating the result of the operation
   */
  @PostMapping("/upload")
  public ResponseEntity<String> uploadCsv(
    @RequestParam("file") MultipartFile file,
    @RequestParam("collectionId") int collectionId
  ) {
    if (file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        "File is empty"
      );
    }
    try {
      List<CsvSnowflakeRow> rows = CsvUtil.parseCsvFile(
        file.getInputStream(),
        CsvSnowflakeRow.class
      );
      csvService.processCsv(rows, collectionId);
      return ResponseEntity.ok(
        "CSV processed successfully. Rows: " + rows.size()
      );
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        "Error processing file: " + e.getMessage()
      );
    }
  }

  /**
   * Endpoint to upload a CSV file containing video data and process its content. The CSV is expected to contain rows of video data that will be processed by the CsvService.
   * @param file The CSV file to upload
   * @param collectionId The collection ID to associate with the processed video rows
   * @return A ResponseEntity indicating the result of the operation
   */
  @PostMapping("/video/upload")
  public ResponseEntity<String> uploadVideo(
    @RequestParam("file") MultipartFile file,
    @RequestParam("collectionId") int collectionId
  ) {
    if (file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        "File is empty"
      );
    }
    try {
      List<CsvVideoRow> rows = CsvUtil.parseCsvFile(
        file.getInputStream(),
        CsvVideoRow.class
      );
      csvService.processVideoCsv(rows, collectionId);
      return ResponseEntity.ok(
        "Video CSV processed successfully. Rows: " + rows.size()
      );
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        "Error processing file: " + e.getMessage()
      );
    }
  }

  @GetMapping("/externalId")
  public ResponseEntity<Map<String, String>> getUUID(
    @RequestParam("externalId") String externalId
  ) {
    Map<String, String> result = csvService.getCashmereId(externalId);
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/delete")
  public ResponseEntity<String> deleteOmnipub(
    @RequestParam("file") MultipartFile file
  ) {
    if (file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        "File is empty"
      );
    }
    try {
      List<CsvDeletionManifestRow> rows = CsvUtil.parseCsvFile(
        file.getInputStream(),
        CsvDeletionManifestRow.class
      );
      if (rows.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
          "CSV file is empty or does not contain valid deletion manifest rows"
        );
      }
      return ResponseEntity.ok(
        "Delete request sent for Cashmere rows: " + rows.size()
      );
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        "Error processing file: " + e.getMessage()
      );
    }
  }
}
