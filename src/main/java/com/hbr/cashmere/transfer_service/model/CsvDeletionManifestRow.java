package com.hbr.cashmere.transfer_service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CsvDeletionManifestRow extends CsvRow {

  private String source;
  private String previousValue;
  private String currentValue;
  private String valueValidFrom;
  private String valueChangeDetectedAt;

  public CsvDeletionManifestRow(
    String coreProductId,
    String availabilityPk,
    String source,
    String previousValue,
    String currentValue,
    String valueValidFrom,
    String valueChangeDetectedAt
  ) {
    super(coreProductId, availabilityPk);
    this.source = source;
    this.previousValue = previousValue;
    this.currentValue = currentValue;
    this.valueValidFrom = valueValidFrom;
    this.valueChangeDetectedAt = valueChangeDetectedAt;
  }
}
