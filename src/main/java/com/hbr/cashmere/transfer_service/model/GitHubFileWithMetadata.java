package com.hbr.cashmere.transfer_service.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubFileWithMetadata {

  private byte[] content;
  private LocalDateTime lastCommitDate;
  private String commitSha;
  private String commitMessage;
}
