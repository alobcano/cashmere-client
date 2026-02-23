package com.hbr.cashmere.client.model;

import java.util.Date;
import lombok.Data;

@Data
public class OmnipubMetadata {
  private String title;
  private String[] authors;
  private String publisher;
  private Date publicationDate;
}
