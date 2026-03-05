package com.hbr.cashmere.transfer_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvVideoRow {

  private String coreProductId;
  private String availabilityPk;
  private String alternateIdType1;
  private String alternateIdValue1;
  private String alternateIdType2;
  private String alternateIdValue2;
  private String title;
  private String copyrightHolder;
  private String aiElegibilitySet;
  private String productType;
}
