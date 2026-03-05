package com.hbr.cashmere.transfer_service.controller;

import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.service.CsvService;
import com.hbr.cashmere.transfer_service.util.CsvUtil;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
      List<CsvRow> rows = CsvUtil.parseCsv(file.getInputStream());
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
      List<CsvVideoRow> rows = CsvUtil.parseVideoCsv(file.getInputStream());
      csvService.processVideoCsv(rows, collectionId);
      return ResponseEntity.ok("Video uploaded successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        "Error processing file: " + e.getMessage()
      );
    }
  }
}
