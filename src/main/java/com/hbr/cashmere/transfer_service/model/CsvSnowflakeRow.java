package com.hbr.cashmere.transfer_service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CsvSnowflakeRow extends CsvRow {

  private String aiEligibilitySet;
  private String s3Path;
  private String copyrightHolderDisplayName;

  public CsvSnowflakeRow(
    String coreProductId,
    String availabilityPk,
    String aiEligibilitySet,
    String s3Path,
    String copyrightHolderDisplayName
  ) {
    super(coreProductId, availabilityPk);
    this.aiEligibilitySet = aiEligibilitySet;
    this.s3Path = s3Path;
    this.copyrightHolderDisplayName = copyrightHolderDisplayName;
  }
}
