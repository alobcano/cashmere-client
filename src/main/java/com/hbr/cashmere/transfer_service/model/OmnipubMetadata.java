package com.hbr.cashmere.transfer_service.model;

import java.util.Date;
import lombok.Data;

@Data
public class OmnipubMetadata {

  private String title;
  private String[] authors;
  private String publisher;
  private Date publicationDate;
}
