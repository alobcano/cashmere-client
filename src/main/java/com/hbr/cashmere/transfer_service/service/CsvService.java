package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.constants.CsvConstants;
import com.hbr.cashmere.transfer_service.constants.DeleteConstants;
import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.model.GitHubFileWithMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.util.CsvUtil;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
public class CsvService {

  private final S3FileService s3FileService;
  private final CashmereService cashmereService;
  private final ContentService contentService;
  private final GitService gitService;

  public CsvService(
    S3FileService s3FileService,
    CashmereService cashmereService,
    ContentService contentService,
    GitService gitService
  ) {
    this.s3FileService = s3FileService;
    this.cashmereService = cashmereService;
    this.contentService = contentService;
    this.gitService = gitService;
  }

  /**
   * Processes a list of CSV rows, downloading XML files from S3, extracting metadata, and creating Omnipubs in Cashmere.
   *
   * @param rows The list of CSV rows to process
   * @param collection The collection name to associate with the created Omnipubs
   * @param isUpdate Flag indicating whether the operation is an update
   */
  public void processCsv(
    List<CsvSnowflakeRow> rows,
    String collection,
    boolean isUpdate
  ) {
    for (CsvSnowflakeRow row : rows) {
      byte[] xmlFile;
      JsonNode metadataNode = null;
      List<String> s3Path = CsvUtil.getS3Parts(row.getS3Path());

      log.info("processing file: {}", s3Path.get(2));
      xmlFile = this.downloadFileFromS3(s3Path);
      if (xmlFile.length == 0) {
        log.error(
          "Failed to download file from S3: s3://{}/{}",
          s3Path.get(0),
          s3Path.get(1)
        );
        continue;
      }

      if (XmlUtil.extractAuthors(xmlFile).length == 0) {
        metadataNode = contentService
          .fetchMetadata(row.getAvailabilityPk())
          .block();
      }

      String cashmereUuid = this.getCashmereId(row.getAvailabilityPk());

      if (isUpdate) {
        if (cashmereUuid != null) {
          OmnipubMetadata metadata =
            metadataNode != null
              ? CsvUtil.getMetadata(xmlFile, metadataNode)
              : CsvUtil.getMetadata(xmlFile);
          cashmereService.updateOmnipub(cashmereUuid, metadata).block();
        } else {
          log.warn(
            "No Omnipub found in Cashmere for availabilityPk: {}. Skipping metadata update.",
            row.getAvailabilityPk()
          );
        }
        continue;
      }
      if (cashmereUuid != null) {
        log.warn(
          "Omnipub already exists in Cashmere for availabilityPk: {}. Skipping creation.",
          row.getAvailabilityPk()
        );
        continue;
      }
      // XmlUtil.saveXmlToFile(xmlFile, s3Path.get(2));
      // log.info("Metadata for {} is: {}", row.getAvailabilityPk(), metadataNode != null ? CsvUtil.getMetadata(xmlFile, metadataNode) : CsvUtil.getMetadata(xmlFile));
      this.createOmnipub(
        metadataNode != null
          ? CsvUtil.getMetadata(xmlFile, metadataNode)
          : CsvUtil.getMetadata(xmlFile),
        xmlFile,
        CsvUtil.getCollectionId(collection),
        row.getAvailabilityPk(),
        s3Path.get(2)
      );
    }
  }

  /**
   * Downloads a file from S3 based on the provided S3 path.
   * @param s3Path The S3 path components (bucket and key)
   * @return The downloaded file as a byte array
   */
  private byte[] downloadFileFromS3(List<String> s3Path) {
    try {
      String bucketName = s3Path.get(0);
      String key = s3Path.get(1);
      return s3FileService.downloadFile(bucketName, key);
    } catch (Exception e) {
      log.error(
        "Error downloading file from S3: s3://{}/{}",
        s3Path.get(0),
        s3Path.get(1),
        e
      );
      return new byte[0];
    }
  }

  /**
   * Creates an Omnipub in Cashmere using the provided metadata and file content.
   * @param metadata The metadata for the Omnipub
   * @param fileContent The XML file content
   * @param collectionId The collection ID to associate with the Omnipub
   * @param externalId The external ID for the Omnipub
   * @param filename The filename for the XML file
   */
  private void createOmnipub(
    OmnipubMetadata metadata,
    byte[] fileContent,
    int collectionId,
    String externalId,
    String filename
  ) {
    try {
      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("collection_ids", collectionId);
      builder.part("external_id", externalId);

      Map<String, Object> metadataMap = Map.of(
        "title",
        metadata.getTitle(),
        "authors",
        metadata.getAuthors(),
        "publisher",
        metadata.getPublisher(),
        "creation_date",
        metadata.getPublicationDate(),
        "updated_date",
        metadata.getLastUpdatedDate()
      );
      builder.part("metadata", metadataMap, MediaType.APPLICATION_JSON);

      Resource resource = new ByteArrayResource(fileContent) {
        @Override
        public String getFilename() {
          return filename;
        }
      };
      builder.part("file", resource, MediaType.APPLICATION_XML);

      cashmereService.createOmnipub(builder.build()).block();
    } catch (Exception e) {
      log.error("Error creating Omnipub for title: {}", metadata.getTitle(), e);
    }
  }

  /**
   * Processes a list of video CSV rows, downloading XML files from Git, extracting metadata, and creating Omnipubs in Cashmere.
   *
   * @param rows The list of video CSV rows to process
   * @param collectionId The collection ID to associate with the Omnipubs
   */
  public void processVideoCsv(
    List<CsvVideoRow> rows,
    String collection,
    boolean isUpdate
  ) {
    for (CsvVideoRow row : rows) {
      log.info(
        "Processing video row with title: {} and external_id: {}",
        row.getTitle(),
        row.getAvailabilityPk()
      );
      String filename = String.format(
        "%s.xml",
        row.getAlternateIdType1().equals("KAL")
          ? row.getAlternateIdValue1()
          : row.getAlternateIdValue2()
      );
      try {
        log.info("processing file: {}", filename);

        GitHubFileWithMetadata fileWithMetadata = gitService
          .downloadXmlWithMetadata(filename)
          .block();
        byte[] xmlFile = fileWithMetadata.getContent();

        JsonNode metadataNode = contentService
          .fetchMetadata(row.getAvailabilityPk())
          .block();

        OmnipubMetadata metadata = CsvUtil.getMetadata(
          fileWithMetadata,
          row.getTitle(),
          metadataNode
        );

        String cashmereUuid = this.getCashmereId(row.getAvailabilityPk());

        if (isUpdate) {
          if (cashmereUuid != null) {
            cashmereService.updateOmnipub(cashmereUuid, metadata).block();
          } else {
            log.warn(
              "No Omnipub found in Cashmere for availabilityPk: {}. Skipping metadata update.",
              row.getAvailabilityPk()
            );
          }
          continue;
        }

        if (cashmereUuid != null) {
          log.warn(
            "Omnipub already exists in Cashmere for availabilityPk: {}. Skipping creation.",
            row.getAvailabilityPk()
          );
          continue;
        }

        this.createOmnipub(
          metadata,
          xmlFile,
          CsvUtil.getCollectionId(collection),
          row.getAvailabilityPk(),
          filename
        );
      } catch (Exception e) {
        log.error("Error processing file: {}", filename, e);
      }
    }
  }

  /**
   * Retrieves the Cashmere UUID for a given external ID by querying the Cashmere service. If an Omnipub with the specified
   * external ID exists, it returns the UUID; otherwise, it returns null.
   *
   * @param externalId The external ID of the Omnipub
   * @return The Cashmere UUID of the Omnipub, or null if not found
   */
  private String getCashmereId(String externalId) {
    JsonNode response = cashmereService.getOmnipubs(externalId).block();
    if (
      response != null &&
      response.has(CsvConstants.ITEMS) &&
      response.get(CsvConstants.ITEMS).isArray() &&
      response.get(CsvConstants.ITEMS).size() > 0
    ) {
      JsonNode firstItem = response.get(CsvConstants.ITEMS).get(0);
      return firstItem.get("uuid").asString();
    }
    return null;
  }

  /**
   * Processes a list of CSV Deletion Manifest rows, retrieving the corresponding Cashmere UUIDs and sending delete requests
   * to the Cashmere service for each Omnipub that matches the availabilityPk in the rows.
   * @param rows The list of CSV Deletion Manifest rows to process
   */
  public void deleteOmnipubs(List<CsvDeletionManifestRow> rows) {
    for (CsvDeletionManifestRow row : rows) {
      if (!checkIfShouldBeDeleted(row)) {
        continue;
      }
      try {
        String cashmereUuid = getCashmereId(row.getAvailabilityPk());
        if (cashmereUuid != null) {
          log.info(
            "Found Omnipub in Cashmere for availabilityPk: {}. Sending delete request",
            row.getAvailabilityPk()
          );
          cashmereService
            .deleteOmnipub(cashmereUuid)
            .subscribe(
              successResponse ->
                log.info(
                  "Successfully sent delete request for Cashmere UUID: {}, coreProductId: {}, availabilityPk: {}. Response: {}",
                  cashmereUuid,
                  row.getCoreProductId(),
                  row.getAvailabilityPk(),
                  successResponse
                ),
              error ->
                log.error(
                  "Error sending delete request for Cashmere UUID: {}, coreProductId: {}, availabilityPk: {}",
                  cashmereUuid,
                  row.getCoreProductId(),
                  row.getAvailabilityPk(),
                  error
                )
            );
        } else {
          log.warn(
            "No Omnipub found in Cashmere for availabilityPk: {}. Skipping deletion.",
            row.getAvailabilityPk()
          );
        }
      } catch (Exception e) {
        log.error(
          "Error processing Deletion Manifest row with coreProductId: {} and availabilityPk: {}",
          row.getCoreProductId(),
          row.getAvailabilityPk(),
          e
        );
      }
    }
  }

  private boolean checkIfShouldBeDeleted(CsvDeletionManifestRow row) {
    boolean isDeleted = false;
    switch (row.getSource()) {
      case DeleteConstants.PRODUCT_STATE:
        isDeleted = !row.getCurrentValue().equals("Approved (All)");
        break;
      case DeleteConstants.STATUS:
        isDeleted = !row.getCurrentValue().equals("C");
        break;
      case DeleteConstants.RESTRICTION_CODE:
        isDeleted = !row.getCurrentValue().equals("99A");
        break;
      case DeleteConstants.AI_ELIGIBILITY_SET:
        isDeleted = row.getCurrentValue().equals("Not Eligible");
        break;
      case DeleteConstants.LDE_ASSET_TYPE_CATEGORY:
        isDeleted = row.getCurrentValue().equals("");
        break;
      default:
        break;
    }
    return isDeleted;
  }
}
