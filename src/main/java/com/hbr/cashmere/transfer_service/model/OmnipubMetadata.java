package com.hbr.cashmere.transfer_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  
  @JsonProperty("creation_date")
  private String publicationDate;

  @JsonProperty("updated_date")
  private String lastUpdatedDate;
}
