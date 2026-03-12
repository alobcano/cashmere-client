package com.hbr.cashmere.transfer_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OmnipubMetadata {

  private String title;
  private String[] authors;
  private String publisher;
  private String publicationDate;
  private String lastUpdatedDate;
}
