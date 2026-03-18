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
        CsvRow row = null;

        switch (columnCount) {
          case 2:
            row = new CsvRow(csvRecord.get(0), csvRecord.get(1));
            break;
          case 4:
            row = new CsvSnowflakeRow(
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3)
            );
            break;
          case 6:
            row = new CsvDeletionManifestRow(
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3),
              csvRecord.get(4),
              csvRecord.get(5)
            );
            break;
          case 10:
            row = new CsvVideoRow(
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
            break;
          default:
            log.warn(
              "Skipping malformed line with {} columns at line {}: {}",
              columnCount,
              csvRecord.getRecordNumber(),
              csvRecord
            );
            break;
        }

        if (row != null && rowType.isInstance(row)) {
          rows.add((T) row);
        } else if (row != null) {
          log.warn(
            "Skipping row of type {} (expected {}) at line {}",
            row.getClass().getSimpleName(),
            rowType.getSimpleName(),
            csvRecord.getRecordNumber()
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

  /**
   * Maps a collection name to its corresponding collection ID.
   * @param collection The name of the collection
   * @return The ID of the collection
   */
  public static int getCollectionId(String collection) {
    switch (collection) {
      case CollectionConstants.CL_ARTICLES_BASE:
        return CollectionConstants.CL_ARTICLES_BASE_ID;
      case CollectionConstants.CL_ARTICLES_DEI:
        return CollectionConstants.CL_ARTICLES_DEI_ID;
      case CollectionConstants.CL_VIDEOS_BASE:
        return CollectionConstants.CL_VIDEOS_BASE_ID;
      case CollectionConstants.CL_VIDEOS_DEI:
        return CollectionConstants.CL_VIDEOS_DEI_ID;
      case CollectionConstants.CL_PODCASTS_BASE:
        return CollectionConstants.CL_PODCASTS_BASE_ID;
      case CollectionConstants.CL_PODCASTS_DEI:
        return CollectionConstants.CL_PODCASTS_DEI_ID;
      default:
        return 0;
    }
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
