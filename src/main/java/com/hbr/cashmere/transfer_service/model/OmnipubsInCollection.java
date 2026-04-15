package com.hbr.cashmere.transfer_service.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OmnipubsInCollection {

  @JsonProperty("publication_uuids")
  private List<String> publicationUuids;
  
}
