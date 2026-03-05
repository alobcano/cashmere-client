package com.hbr.cashmere.transfer_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvRow {

  private String coreProductId;
  private String availabilityPk;
  private String aiEligibilitySet;
  private String s3Path;
}
