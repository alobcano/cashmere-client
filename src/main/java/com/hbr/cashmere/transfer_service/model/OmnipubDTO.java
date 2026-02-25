package com.hbr.cashmere.transfer_service.model;

import lombok.Data;

@Data
public class OmnipubDTO {

  private int[] collectionIds;
  private String externalId;
  private OmnipubMetadata metadata;
  private byte[] file;
}
