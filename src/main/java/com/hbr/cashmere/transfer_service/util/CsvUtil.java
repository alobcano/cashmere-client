package com.hbr.cashmere.transfer_service.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvUtil {

  private CsvUtil() {}

  public static List<CsvRow> parseCsv(InputStream inputStream) {
    List<CsvRow> rows = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(inputStream, StandardCharsets.UTF_8)
    )) {
      String line;
      boolean isFirstLine = true;
      while ((line = reader.readLine()) != null) {
        if (isFirstLine) {
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
        } else {
          log.warn("Skipping malformed line: {}", line);
        }
      }
    } catch (Exception e) {
      log.error("Error parsing CSV", e);
    }
    return rows;
  }

  public static List<CsvVideoRow> parseVideoCsv(InputStream inputStream) {
    List<CsvVideoRow> rows = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(inputStream, StandardCharsets.UTF_8)
    )) {
      String line;
      boolean isFirstLine = true;
      while ((line = reader.readLine()) != null) {
        if (isFirstLine) {
          isFirstLine = false;
          continue;
        }
        String[] columns = line.split(",");
        if (columns.length == 10) {
          CsvVideoRow row = new CsvVideoRow(
            columns[0].trim(),
            columns[1].trim(),
            columns[2].trim(),
            columns[3].trim(),
            columns[4].trim(),
            columns[5].trim(),
            columns[6].trim(),
            columns[7].trim(),
            columns[8].trim(),
            columns[9].trim()
          );
          rows.add(row);
        } else {
          log.warn("Skipping malformed line: {}", line);
        }
      }
    } catch (Exception e) {
      log.error("Error parsing CSV", e);
    }
    return rows;
  }

}
