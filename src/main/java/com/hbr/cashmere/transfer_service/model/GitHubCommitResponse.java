package com.hbr.cashmere.transfer_service.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubCommitResponse {

  private String sha;
  private Commit commit;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Commit {

    private String message;
    private Committer committer;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Committer {

    private String name;
    private String email;
    private LocalDateTime date;
  }
}
