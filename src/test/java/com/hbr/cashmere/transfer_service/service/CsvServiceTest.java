package com.hbr.cashmere.transfer_service.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hbr.cashmere.transfer_service.configuration.CollectionIdProperties;
import com.hbr.cashmere.transfer_service.model.CsvDeletionManifestRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CsvService.
 * 
 * Note: This test class focuses on testing input validation and business logic that doesn't
 * involve async operations. Full end-to-end tests for processCsv, processVideoCsv, and the
 * complete deleteOmnipubs flow would be better suited as integration tests due to:
 * 1. Heavy dependencies on static utility methods (XmlUtil, CsvUtil) 
 * 2. Async subscribe() operations that don't block
 * 3. Complex reactive stream orchestration
 */
@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

  @Mock
  private S3FileService s3FileService;

  @Mock
  private CashmereService cashmereService;

  @Mock
  private ContentService contentService;

  @Mock
  private GitService gitService;

  @Mock
  private tools.jackson.databind.ObjectMapper objectMapper;

  @Mock
  private CollectionIdProperties collectionIdProperties;

  private CsvService csvService;

  @BeforeEach
  void setUp() {
    csvService = new CsvService(
      s3FileService,
      cashmereService,
      contentService,
      gitService,
      objectMapper,
      collectionIdProperties
    );
  }

  @Test
  void deleteOmnipubs_ShouldNotDeleteWhen_ProductStateIsApproved() {
    // Given - currentValue is "Approved (All)" so it should NOT be deleted
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "PRODUCT_STATE",
      "Deleted",
      "Approved (All)",
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then - should not make any API calls since item shouldn't be deleted
    verify(cashmereService, never()).getOmnipubs(anyString());
    verify(cashmereService, never()).deleteOmnipub(anyString());
  }

  @Test
  void deleteOmnipubs_ShouldNotDelete_StatusIsC() {
    // Given - STATUS is "C", so should NOT be deleted
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "STATUS",
      "D",
      "C", // Current value is "C", so do not delete
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then
    verify(cashmereService, never()).getOmnipubs(anyString());
  }

  @Test
  void deleteOmnipubs_ShouldNotDelete_RestrictionCodeIs99A() {
    // Given - RESTRICTION_CODE is "99A", so should NOT be deleted
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "RESTRICTION_CODE",
      "00A",
      "99A", // Current value is "99A", so do not delete
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then
    verify(cashmereService, never()).getOmnipubs(anyString());
  }

  @Test
  void deleteOmnipubs_ShouldNotDelete_AIEligibilityIsEligible() {
    // Given - AI_ELIGIBILITY_SET is "Eligible", so should NOT be deleted
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "AI_ELIGIBILITY_SET",
      "Not Eligible",
      "Eligible", // Current value is "Eligible", so do not delete
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then
    verify(cashmereService, never()).getOmnipubs(anyString());
  }

  @Test
  void deleteOmnipubs_ShouldNotDelete_LDEAssetHasValue() {
    // Given - LDE_ASSET_TYPE_CATEGORY has a value, so should NOT be deleted
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "LDE_ASSET_TYPE_CATEGORY",
      "",
      "Category", // Current value is not empty, so do not delete
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then
    verify(cashmereService, never()).getOmnipubs(anyString());
  }

  @Test
  void deleteOmnipubs_HandlesUnknownSourceGracefully() {
    // Given - unknown source type
    CsvDeletionManifestRow row = new CsvDeletionManifestRow(
      "product-1",
      "pk-1",
      "UNKNOWN_SOURCE",
      "oldValue",
      "newValue",
      "2024-01-01",
      "2024-01-15"
    );

    // When
    csvService.deleteOmnipubs(java.util.List.of(row));

    // Then - should not throw exception, just skip the row
    verify(cashmereService, never()).getOmnipubs(anyString());
  }
}
