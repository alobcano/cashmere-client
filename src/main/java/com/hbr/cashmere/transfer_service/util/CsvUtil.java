package com.hbr.cashmere.transfer_service.util;

import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvUtil {

  private CsvUtil() {}

  @SuppressWarnings("unchecked")
  public static <T extends CsvRow> List<T> parseCsvFile(
    InputStream inputStream,
    Class<T> rowType
  ) {
    List<T> rows = new ArrayList<>();
    try (
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.UTF_8)
      )
    ) {
      String line;
      boolean isFirstLine = true;
      while ((line = reader.readLine()) != null) {
        if (isFirstLine) {
          isFirstLine = false;
          continue;
        }
        String[] columns = line.split(",");
        int columnCount = columns.length;
        CsvRow row = null;

        switch (columnCount) {
          case 2:
            row = new CsvRow(columns[0].trim(), columns[1].trim());
            break;
          case 4:
            row = new CsvSnowflakeRow(
              columns[0].trim(),
              columns[1].trim(),
              columns[2].trim(),
              columns[3].trim()
            );
            break;
          case 6:
            row = new CsvDeletionManifestRow(
              columns[0].trim(),
              columns[1].trim(),
              columns[2].trim(),
              columns[3].trim(),
              columns[4].trim(),
              columns[5].trim()
            );
            break;
          case 10:
            row = new CsvVideoRow(
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
            break;
          default:
            log.warn(
              "Skipping malformed line with {} columns: {}",
              columnCount,
              line
            );
            break;
        }

        if (row != null && rowType.isInstance(row)) {
          rows.add((T) row);
        } else if (row != null) {
          log.warn(
            "Skipping row of type {} (expected {})",
            row.getClass().getSimpleName(),
            rowType.getSimpleName()
          );
        }
      }
    } catch (Exception e) {
      log.error("Error parsing CSV", e);
    }
    return rows;
  }

  // Convenience method for backward compatibility
  public static List<CsvRow> parseCsvFile(InputStream inputStream) {
    return parseCsvFile(inputStream, CsvRow.class);
  }
}
