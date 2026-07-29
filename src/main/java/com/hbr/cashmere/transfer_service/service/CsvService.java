package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.constants.CollectionConstants;
import com.hbr.cashmere.transfer_service.constants.CsvConstants;
import com.hbr.cashmere.transfer_service.constants.DeleteConstants;
import com.hbr.cashmere.transfer_service.constants.XmlConstants;
import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import com.hbr.cashmere.transfer_service.model.CsvSnowflakeRow;
import com.hbr.cashmere.transfer_service.model.CsvVideoRow;
import com.hbr.cashmere.transfer_service.model.GitHubFileWithMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubsInCollection;
import com.hbr.cashmere.transfer_service.util.CsvUtil;
import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class CsvService {

  private final S3FileService s3FileService;
  private final CashmereService cashmereService;
  private final ContentService contentService;
  private final GitService gitService;
  private final ObjectMapper objectMapper;

  public CsvService(
      S3FileService s3FileService,
      CashmereService cashmereService,
      ContentService contentService,
      GitService gitService,
      ObjectMapper objectMapper) {
    this.s3FileService = s3FileService;
    this.cashmereService = cashmereService;
    this.contentService = contentService;
    this.gitService = gitService;
    this.objectMapper = objectMapper;
  }

  /**
   * Processes a list of CSV rows, downloading XML files from S3, extracting metadata, and creating
   * Omnipubs in Cashmere.
   *
   * @param rows The list of CSV rows to process
   * @param collectionId The collection name to associate with the created Omnipubs
   * @param isUpdate Flag indicating whether the operation is an update
   */
  public void processCsv(List<CsvSnowflakeRow> rows, int collectionId, boolean isUpdate) {
    for (CsvSnowflakeRow row : rows) {
      byte[] xmlFile;
      JsonNode metadataNode = null;
      List<String> s3Path = new ArrayList<>();
      // List<String> s3Path = CsvUtil.getS3Parts(row.getS3Path());
      s3Path.add("hbrg-prod");
      s3Path.add(row.getS3Path() + ".xml");
      s3Path.add(row.getS3Path().substring(row.getS3Path().lastIndexOf("/") + 1) + ".xml");

      log.info("processing file: {}", s3Path.get(2));
      xmlFile = this.downloadFileFromS3(s3Path);
      if (xmlFile.length == 0) {
        log.error("Failed to download file from S3: s3://{}/{}", s3Path.get(0), s3Path.get(1));
        continue;
      }

      if (XmlUtil.extractAuthors(xmlFile).length == 0) {
        metadataNode = contentService.fetchMetadata(row.getAvailabilityPk()).block();
      }

      String cashmereUuid = this.getCashmereId(row.getAvailabilityPk());

      if (isUpdate) {
        this.updateOmnipubMetadata(
            cashmereUuid,
            metadataNode,
            xmlFile,
            row.getAvailabilityPk(),
            row.getCopyrightHolderDisplayName());
        continue;
      }
      if (cashmereUuid != null) {
        log.warn(
            "Omnipub already exists in Cashmere for availabilityPk: {}. Skipping creation.",
            row.getAvailabilityPk());
//        this.updateOmnipubCollection(cashmereUuid, collectionId, row.getAvailabilityPk());
//        this.updateOmnipubContent(
//            xmlFile,
//            cashmereUuid,
//            row.getAvailabilityPk(),
//            metadataNode,
//            s3Path.get(2),
//            collectionId,
//            row.getCopyrightHolderDisplayName());
        continue;
      }

       this.createOmnipub(
          metadataNode != null
              ? CsvUtil.getMetadata(xmlFile, metadataNode, row.getCopyrightHolderDisplayName())
              : CsvUtil.getMetadata(xmlFile, row.getCopyrightHolderDisplayName()),
          xmlFile,
          collectionId,
          row.getAvailabilityPk(),
          s3Path.get(2));
    }
  }

  private void updateOmnipubContent(
      byte[] xmlFile,
      String cashmereUuid,
      String availabilityPk,
      JsonNode metadataNode,
      String filename,
      int collectionId,
      String copyrightHolderDisplayName) {
    try {
      JsonNode omnipub = cashmereService.getOmnipub(cashmereUuid).block();
      if (omnipub != null && omnipub.has("data") && omnipub.get("data").has("updated_date")) {
        String existingUpdatedDate = omnipub.get("data").get("updated_date").asString();
        String newUpdatedDate = XmlUtil.extractDate(xmlFile, XmlConstants.UPDATED_TAG);
        if (newUpdatedDate != null && newUpdatedDate.compareTo(existingUpdatedDate) > 0) {
          log.info(
              "Updating content for Omnipub with UUID: {}. Existing updated_date: {}, New"
                  + " updated_date: {}",
              cashmereUuid,
              existingUpdatedDate,
              newUpdatedDate);
          cashmereService.deleteOmnipub(cashmereUuid).block();
          this.createOmnipub(
              metadataNode != null
                  ? CsvUtil.getMetadata(xmlFile, metadataNode, copyrightHolderDisplayName)
                  : CsvUtil.getMetadata(xmlFile, copyrightHolderDisplayName),
              xmlFile,
              collectionId,
              availabilityPk,
              filename);
        }
        log.info(
            "Skipping content update for Omnipub with UUID: {} as existing updated_date: {} is more"
                + " recent than new updated_date: {}",
            cashmereUuid,
            existingUpdatedDate,
            newUpdatedDate);
      } else {
        log.warn(
            "No metadata found for Omnipub with UUID: {}. Skipping content update for"
                + " availabilityPk: {}.",
            cashmereUuid,
            availabilityPk);
      }
    } catch (Exception e) {
      log.error(
          "Error fetching Omnipub details for Cashmere UUID: {}, availabilityPk: {}",
          cashmereUuid,
          availabilityPk,
          e);
    }
  }

  /**
   * Updates the metadata of an existing Omnipub in Cashmere. If the cashmereUuid is null, it logs a
   * warning and skips the update.
   *
   * @param cashmereUuid The UUID of the Omnipub in Cashmere
   * @param metadataNode The metadata node containing updated information
   * @param xmlFile The XML file content
   * @param availabilityPk The availability primary key
   */
  private void updateOmnipubMetadata(
      String cashmereUuid,
      JsonNode metadataNode,
      byte[] xmlFile,
      String availabilityPk,
      String copyrightHolderDisplayName) {
    if (cashmereUuid != null) {
      OmnipubMetadata metadata =
          metadataNode != null
              ? CsvUtil.getMetadata(xmlFile, metadataNode, copyrightHolderDisplayName)
              : CsvUtil.getMetadata(xmlFile, copyrightHolderDisplayName);
      cashmereService.updateOmnipub(cashmereUuid, metadata).block();
    } else {
      log.warn(
          "No Omnipub found in Cashmere for availabilityPk: {}. Skipping metadata update.",
          availabilityPk);
    }
  }

  private void updateOmnipubCollection(
      String cashmereUuid, int collectionId, String availabilityPk) {
    for (Map.Entry<String, Integer> entry : CollectionConstants.COLLECTION_NAME_TO_ID.entrySet()) {
      if (entry.getValue() == collectionId) {
        continue;
      }
      JsonNode response = cashmereService.getOmnipubs(availabilityPk, entry.getValue()).block();
      if (response != null
          && response.has(CsvConstants.ITEMS)
          && response.get(CsvConstants.ITEMS).isArray()
          && !response.get(CsvConstants.ITEMS).isEmpty()) {
        log.info(
            "Updating collection from {} to {} for externalID {}",
            entry.getKey(),
            CollectionConstants.COLLECTION_ID_TO_NAME.get(collectionId),
            availabilityPk);

        OmnipubsInCollection omnipubToRemove = new OmnipubsInCollection(List.of(cashmereUuid));
        cashmereService.removeOmnipubFromCollection(omnipubToRemove, entry.getValue()).block();
        cashmereService.addOmnipubToCollection(omnipubToRemove, collectionId).block();
        return;
      }
    }
    log.warn("Skipping collection update for externalId: {}.", availabilityPk);
  }

  /**
   * Downloads a file from S3 based on the provided S3 path.
   *
   * @param s3Path The S3 path components (bucket and key)
   * @return The downloaded file as a byte array
   */
  private byte[] downloadFileFromS3(List<String> s3Path) {
    try {
      String bucketName = s3Path.get(0);
      String key = s3Path.get(1);
      return s3FileService.downloadFile(bucketName, key);
    } catch (Exception e) {
      log.error("Error downloading file from S3: s3://{}/{}", s3Path.get(0), s3Path.get(1), e);
      return new byte[0];
    }
  }

  /**
   * Creates an Omnipub in Cashmere using the provided metadata and file content.
   *
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
      String filename) {
    try {
      if (metadata == null || fileContent == null || fileContent.length == 0) {
        log.error("Invalid createOmnipub request. metadata or fileContent is missing.");
        return;
      }

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("collection_ids", collectionId);
      builder.part("external_id", externalId);

      Map<String, Object> metadataMap =
          Map.of(
              "title",
              metadata.getTitle(),
              "authors",
              metadata.getAuthors(),
              "publisher",
              metadata.getPublisher(),
              "creation_date",
              metadata.getPublicationDate(),
              "updated_date",
              metadata.getLastUpdatedDate());
      builder.part("metadata", metadataMap, MediaType.APPLICATION_JSON);

      Resource resource =
          new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
              return filename;
            }
          };
      builder.part("file", resource, MediaType.APPLICATION_XML);

      String response = cashmereService.createOmnipub(builder.build()).block();


//      String uuid = null;
//      if (response != null && !response.isEmpty()) {
//        try {
//          JsonNode responseNode = objectMapper.readTree(response);
//          if (responseNode.has("uuid")) {
//            uuid = responseNode.get("uuid").asString();
//            log.info("Successfully created Omnipub with UUID: {}", uuid);
//          }
//        } catch (Exception e) {
//          log.error("Failed to parse createOmnipub response: {}", response, e);
//        }
//      }
//
//
//      try {
//        log.info("Waiting 10 seconds before checking for duplicates...");
//        Thread.sleep(4000);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        log.warn("Interrupted while waiting to check for duplicates", e);
//      }
//
//      checkIfOmnipubIsDuplicated(uuid);

    } catch (Exception e) {
      log.error("Error creating Omnipub for title: {}", metadata.getTitle(), e);
    }
  }

  private void checkIfOmnipubIsDuplicated(String omnipubUuid) {
    JsonNode omnipubStatus = cashmereService.getOmnipubStatus(omnipubUuid).block();
    if(omnipubStatus != null && omnipubStatus.has("duplicate_uuid")) {
      log.info("Omnipub with UUID: {} is a duplicate. Deleting the duplicate Omnipub.", omnipubUuid);
      String duplicateUuid = omnipubStatus.get("duplicate_uuid").asString();
      log.info("Deleting Omnipub with UUID: {}", duplicateUuid);
      cashmereService.deleteOmnipub(duplicateUuid).block();
    }
  }

  /**
   * Processes a list of video CSV rows, downloading XML files from Git, extracting metadata, and
   * creating Omnipubs in Cashmere.
   *
   * @param rows The list of video CSV rows to process
   * @param collectionId The collection ID to associate with the Omnipubs
   */
  public void processVideoCsv(List<CsvVideoRow> rows, int collectionId, boolean isUpdate) {
    for (CsvVideoRow row : rows) {
      log.info(
          "Processing video row with title: {} and external_id: {}",
          row.getTitle(),
          row.getAvailabilityPk());
      String filename =
          String.format(
              "%s.xml",
              row.getAlternateIdType1().equals("KAL")
                  ? row.getAlternateIdValue1()
                  : row.getAlternateIdValue2());
      try {
        log.info("processing file: {}", filename);

        GitHubFileWithMetadata fileWithMetadata =
            gitService.downloadXmlWithMetadata(filename).block();
        byte[] xmlFile = XmlUtil.replaceAssetNameTag(fileWithMetadata.getContent(), row.getTitle());

        JsonNode metadataNode = contentService.fetchMetadata(row.getAvailabilityPk()).block();

        OmnipubMetadata metadata =
            CsvUtil.getMetadata(
                fileWithMetadata,
                row.getTitle(),
                metadataNode,
                row.getCopyrightHolderDisplayName());

        String cashmereUuid = this.getCashmereId(row.getAvailabilityPk());

        if (isUpdate) {
          if (cashmereUuid != null) {
            cashmereService.updateOmnipub(cashmereUuid, metadata).block();
          } else {
            log.warn(
                "No Omnipub found in Cashmere for availabilityPk: {}. Skipping metadata update.",
                row.getAvailabilityPk());
          }
          continue;
        }

        if (cashmereUuid != null) {
          log.warn(
              "Omnipub already exists in Cashmere for availabilityPk: {}. Skipping creation.",
              row.getAvailabilityPk());
          // this.updateOmnipubCollection(cashmereUuid, collectionId, row.getAvailabilityPk());
          this.updateOmnipubVideoContent(
              xmlFile, cashmereUuid, row.getAvailabilityPk(), metadata, filename, collectionId);
          continue;
        }

        this.createOmnipub(metadata, xmlFile, collectionId, row.getAvailabilityPk(), filename);
      } catch (Exception e) {
        log.error("Error processing file: {}", filename, e);
      }
    }
  }

  private void updateOmnipubVideoContent(
      byte[] xmlFile,
      String cashmereUuid,
      String availabilityPk,
      OmnipubMetadata metadata,
      String filename,
      int collectionId) {
    try {
      cashmereService.deleteOmnipub(cashmereUuid).block();
      this.createOmnipub(metadata, xmlFile, collectionId, availabilityPk, filename);
    } catch (Exception e) {
      log.error(
          "Error updating video content for Omnipub with UUID: {}, availabilityPk: {}",
          cashmereUuid,
          availabilityPk,
          e);
    }
  }

  /**
   * Retrieves the Cashmere UUID for a given external ID by querying the Cashmere service. If an
   * Omnipub with the specified external ID exists, it returns the UUID; otherwise, it returns null.
   *
   * @param externalId The external ID of the Omnipub
   * @return The Cashmere UUID of the Omnipub, or null if not found
   */
  private String getCashmereId(String externalId) {
    JsonNode response = cashmereService.getOmnipubs(externalId).block();
    if (response != null
        && response.has(CsvConstants.ITEMS)
        && response.get(CsvConstants.ITEMS).isArray()
        && !response.get(CsvConstants.ITEMS).isEmpty()) {
      JsonNode firstItem = response.get(CsvConstants.ITEMS).get(0);
      return firstItem.get("uuid").asString();
    }
    return null;
  }

  /**
   * Processes a list of CSV Deletion Manifest rows, retrieving the corresponding Cashmere UUIDs and
   * sending delete requests to the Cashmere service for each Omnipub that matches the
   * availabilityPk in the rows.
   *
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
              row.getAvailabilityPk());
          cashmereService
              .deleteOmnipub(cashmereUuid)
              .subscribe(
                  successResponse ->
                      log.info(
                          "Successfully sent delete request for Cashmere UUID: {}, coreProductId:"
                              + " {}, availabilityPk: {}. Response: {}",
                          cashmereUuid,
                          row.getCoreProductId(),
                          row.getAvailabilityPk(),
                          successResponse),
                  error ->
                      log.error(
                          "Error sending delete request for Cashmere UUID: {}, coreProductId: {},"
                              + " availabilityPk: {}",
                          cashmereUuid,
                          row.getCoreProductId(),
                          row.getAvailabilityPk(),
                          error));
        } else {
          log.warn(
              "No Omnipub found in Cashmere for availabilityPk: {}. Skipping deletion.",
              row.getAvailabilityPk());
        }
      } catch (Exception e) {
        log.error(
            "Error processing Deletion Manifest row with coreProductId: {} and availabilityPk: {}",
            row.getCoreProductId(),
            row.getAvailabilityPk(),
            e);
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
        isDeleted = row.getCurrentValue().isEmpty();
        break;
      default:
        break;
    }
    return isDeleted;
  }

  public void processLocalXmlFiles(MultipartFile file) {
    byte[] xmlFile;
    JsonNode metadataNode = null;
    try {
      xmlFile = file.getInputStream().readAllBytes();
      if (xmlFile.length == 0) {
        log.error("Failed to read XML file content for file: {}", file.getOriginalFilename());
        return;
      }

      xmlFile = XmlUtil.replaceAssetNameTag(file.getBytes(), "Flipping Imposter Syndrome");
      XmlUtil.saveXmlToFile(xmlFile, file.getOriginalFilename());
      // if (XmlUtil.extractAuthors(xmlFile).length == 0) {
      //   metadataNode = contentService.fetchMetadata(null).block();
      // }

      // String cashmereUuid = this.getCashmereId(null);

      // if (cashmereUuid != null) {
      //   this.updateOmnipubContent(
      //       xmlFile,
      //       cashmereUuid,
      //       null,
      //       metadataNode,
      //       file.getOriginalFilename(),
      //       null,
      //       "Unknown Copyright Holder");
      // }
    } catch (Exception e) {
      log.error(
          "Error processing local XML file: {} with availabilityId: {}",
          file.getOriginalFilename(),
          null,
          e);
    }
  }
}
