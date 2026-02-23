package com.hbr.cashmere.client.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.hbr.cashmere.client.model.CsvRow;
import com.hbr.cashmere.client.service.CsvService;

@RestController
@RequestMapping("/api/csv")
public class CsvUploadController {

  private final CsvService csvService;
  
  public CsvUploadController(CsvService csvService) {
    this.csvService = csvService;
  }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file, @RequestParam("collectionId") String collectionId) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            List<CsvRow> rows = new ArrayList<>();
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) { // skip header
                    isFirstLine = false;
                    continue;
                }
                String[] columns = line.split(",");
                if (columns.length == 4) {
                    CsvRow row = new CsvRow(
                        columns[0].trim(),
                        columns[1].trim(),
                        columns[2].trim(),
                        columns[3].trim()
                    );
                    rows.add(row);
                }
            }
            csvService.processCsv(rows);
            return ResponseEntity.ok("CSV processed successfully. Rows: " + rows.size());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
        }
    }
}
