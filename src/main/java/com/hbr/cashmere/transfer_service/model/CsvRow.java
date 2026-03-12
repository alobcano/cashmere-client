package com.hbr.cashmere.transfer_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CsvRow {

  private String coreProductId;
  private String availabilityPk;
}
