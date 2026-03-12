package com.hbr.cashmere.transfer_service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CsvDeletionManifestRow extends CsvRow {

  private String dbtUpdatedAt;
  private String dbtValidFrom;
  private String dbtValidTo;
  private String deletedDetectedAt;

  public CsvDeletionManifestRow(
    String coreProductId,
    String availabilityPk,
    String dbtUpdatedAt,
    String dbtValidFrom,
    String dbtValidTo,
    String deletedDetectedAt
  ) {
    super(coreProductId, availabilityPk);
    this.dbtUpdatedAt = dbtUpdatedAt;
    this.dbtValidFrom = dbtValidFrom;
    this.dbtValidTo = dbtValidTo;
    this.deletedDetectedAt = deletedDetectedAt;
  }
}
