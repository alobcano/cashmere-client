package com.hbr.cashmere.transfer_service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CsvVideoRow extends CsvRow {

  private String alternateIdType1;
  private String alternateIdValue1;
  private String alternateIdType2;
  private String alternateIdValue2;
  private String title;
  private String copyrightHolder;
  private String aiEligibilitySet;
  private String productType;
  private String copyrightHolderDisplayName;

  public CsvVideoRow(
    String coreProductId,
    String availabilityPk,
    String alternateIdType1,
    String alternateIdValue1,
    String alternateIdType2,
    String alternateIdValue2,
    String title,
    String copyrightHolder,
    String aiEligibilitySet,
    String productType,
    String copyrightHolderDisplayName
  ) {
    super(coreProductId, availabilityPk);
    this.alternateIdType1 = alternateIdType1;
    this.alternateIdValue1 = alternateIdValue1;
    this.alternateIdType2 = alternateIdType2;
    this.alternateIdValue2 = alternateIdValue2;
    this.title = title;
    this.copyrightHolder = copyrightHolder;
    this.aiEligibilitySet = aiEligibilitySet;
    this.productType = productType;
    this.copyrightHolderDisplayName = copyrightHolderDisplayName;
  }
}
