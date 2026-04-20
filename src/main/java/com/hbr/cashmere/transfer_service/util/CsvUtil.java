package com.hbr.cashmere.transfer_service.util;

import com.hbr.cashmere.transfer_service.constants.CollectionConstants;
import com.hbr.cashmere.transfer_service.constants.XmlConstants;
import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.model.GitHubFileWithMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import tools.jackson.databind.JsonNode;

@Slf4j
public class CsvUtil {

  private CsvUtil() {}

  /**
   * Parses a CSV file from the given InputStream and maps each row to an instance of the specified row type (CsvRow, CsvSnowflakeRow, CsvDeletionManifestRow, or CsvVideoRow).
   * The method handles different column counts to determine the appropriate row type and logs any malformed lines or type mismatches.
   * Properly handles quoted fields that may contain commas.
   * @param <T> The type of CSV row to map to
   * @param inputStream The InputStream of the CSV file
   * @param rowType The class of the CSV row type
   * @return A list of CSV rows of the specified type
   */
  @SuppressWarnings("unchecked")
  public static <T extends CsvRow> List<T> parseCsvFile(
    InputStream inputStream,
    Class<T> rowType
  ) {
    List<T> rows = new ArrayList<>();
    try (
      Reader reader = new InputStreamReader(
        inputStream,
        StandardCharsets.UTF_8
      );
      CSVParser csvParser = new CSVParser(
        reader,
        CSVFormat.DEFAULT.builder()
          .setHeader()
          .setSkipHeaderRecord(true)
          .setTrim(true)
          .build()
      )
    ) {
      for (CSVRecord csvRecord : csvParser) {
        int columnCount = csvRecord.values().length;
        T row = null;

        if (rowType.equals(CsvRow.class)) {
          if (columnCount == 2) {
            row = (T) new CsvRow(csvRecord.get(0), csvRecord.get(1));
          } else {
            log.warn(
              "Expected 2 columns for CsvRow but got {} at line {}: {}",
              columnCount,
              csvRecord.getRecordNumber(),
              csvRecord
            );
          }
        } else if (rowType.equals(CsvSnowflakeRow.class)) {
          if (columnCount == 4) {
            row = (T) new CsvSnowflakeRow(
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3)
            );
          } else {
            log.warn(
              "Expected 4 columns for CsvSnowflakeRow but got {} at line {}: {}",
              columnCount,
              csvRecord.getRecordNumber(),
              csvRecord
            );
          }
        } else if (rowType.equals(CsvDeletionManifestRow.class)) {
          if (columnCount == 7) {
            row = (T) new CsvDeletionManifestRow(
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3),
              csvRecord.get(4),
              csvRecord.get(5),
              csvRecord.get(6)
            );
          } else {
            log.warn(
              "Expected 7 columns for CsvDeletionManifestRow but got {} at line {}: {}",
              columnCount,
              csvRecord.getRecordNumber(),
              csvRecord
            );
          }
        } else if (rowType.equals(CsvVideoRow.class)) {
          if (columnCount == 10) {
            row = (T) new CsvVideoRow(
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3),
              csvRecord.get(4),
              csvRecord.get(5),
              csvRecord.get(6),
              csvRecord.get(7),
              csvRecord.get(8),
              csvRecord.get(9)
            );
          } else {
            log.warn(
              "Expected 10 columns for CsvVideoRow but got {} at line {}: {}",
              columnCount,
              csvRecord.getRecordNumber(),
              csvRecord
            );
          }
        } else {
          log.warn(
            "Unsupported row type: {} at line {}",
            rowType.getSimpleName(),
            csvRecord.getRecordNumber()
          );
        }

        if (row != null) {
          rows.add(row);
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

  /**
   * Determines the collection ID based on the filename by checking if the filename contains any of the known collection names. Returns the corresponding collection ID if a match is found, or null if no match is found or if the filename is null.
   * @param filename The name of the file
   * @return The ID of the collection, or 0 if no match is found
   */
  public static int determineCollectionId(String filename) {
  if (filename == null) {
    return 0;
  }
  
  return CollectionConstants.COLLECTION_NAME_TO_ID.entrySet().stream()
    .filter(entry -> filename.contains(entry.getKey()))
    .map(Map.Entry::getValue)
    .findFirst()
    .orElse(0);
}

  public static String[] getAuthors(String authors) {
    if (authors == null || authors.isEmpty()) {
      return new String[0];
    }
    return authors.split(";");
  }

  /**
   * Extracts metadata from the XML file content to create an OmnipubMetadata object.
   *
   * @param fileContent The XML file content
   * @return An OmnipubMetadata object containing the extracted metadata
   */
  public static OmnipubMetadata getMetadata(byte[] fileContent) {
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
   * Extracts metadata from the XML file content and a JsonNode to create an OmnipubMetadata object.
   * @param fileContent The XML file content
   * @param node The JsonNode containing additional metadata
   * @return An OmnipubMetadata object containing the extracted metadata
   */
  public static OmnipubMetadata getMetadata(byte[] fileContent, JsonNode node) {
    JsonNode metadata = node.get("availabilities").get(0);
    OmnipubMetadata omnipubMetadata = new OmnipubMetadata();
    omnipubMetadata.setTitle(XmlUtil.extractTitle(fileContent));
    omnipubMetadata.setAuthors(getAuthors(metadata.get("author").asString()));
    omnipubMetadata.setPublisher("Harvard Business School Publishing - HBD");
    omnipubMetadata.setPublicationDate(
      XmlUtil.extractDate(fileContent, XmlConstants.PUBLISHED_TAG)
    );
    omnipubMetadata.setLastUpdatedDate(
      XmlUtil.extractDate(fileContent, XmlConstants.UPDATED_TAG)
    );
    return omnipubMetadata;
  }

  /**
   * Extracts metadata from the given GitHub file and content metadata to create an OmnipubMetadata object.
   * @param file The GitHub file with metadata
   * @param title The title of the Omnipub
   * @param contentMetadata The content metadata from GitHub
   * @return An OmnipubMetadata object containing the extracted metadata
   */
  public static OmnipubMetadata getMetadata(
    GitHubFileWithMetadata file,
    String title,
    JsonNode contentMetadata
  ) {
    JsonNode metadata = contentMetadata.get("availabilities").get(0);
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss.SSS"
    );
    String publicationDate = LocalDateTime.parse(
      metadata.get("publicationDate").asString(),
      inputFormatter
    )
      .atOffset(ZoneOffset.UTC)
      .format(DateTimeFormatter.ISO_INSTANT);

    OmnipubMetadata omnipubMetadata = new OmnipubMetadata();
    omnipubMetadata.setTitle(title);
    omnipubMetadata.setAuthors(getAuthors(metadata.get("author").asString()));
    omnipubMetadata.setPublisher(
      "Harvard Business School Publishing - Corporate Learning"
    );
    omnipubMetadata.setPublicationDate(publicationDate);
    omnipubMetadata.setLastUpdatedDate(
      file
        .getLastCommitDate()
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_INSTANT)
    );

    return omnipubMetadata;
  }

  /**
   * Extracts the S3 bucket name, key, and filename from the given S3 path.
   * @param s3Path The S3 path in the format "s3://bucket/key"
   * @return A list containing the bucket name, key, and filename
   */
  public static List<String> getS3Parts(String s3Path) {
    String[] parts = s3Path.replace("s3://", "").split("/", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid S3 path: " + s3Path);
    }
    String fileName = parts[1].substring(parts[1].lastIndexOf('/') + 1);
    return List.of(parts[0], parts[1], fileName);
  }
}
