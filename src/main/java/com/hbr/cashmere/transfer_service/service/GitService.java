package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.model.GitHubCommitResponse;
import com.hbr.cashmere.transfer_service.model.GitHubFileWithMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GitService {

  private final WebClient webClient;

  public GitService(@Qualifier("githubWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<byte[]> downloadXml(String fileName) {
    return webClient
      .get()
      .uri(uriBuilder ->
        uriBuilder
          .path(
            "/repos/Corporate-Learning/cl-content-media/contents/content/video/{fileName}"
          )
          .queryParam("ref", "prod-release")
          .build(fileName)
      )
      .retrieve()
      .bodyToMono(byte[].class);
  }

  public Mono<GitHubFileWithMetadata> downloadXmlWithMetadata(String fileName) {
    String filePath = "content/video/" + fileName;

    // Fetch file content
    Mono<byte[]> contentMono = webClient
      .get()
      .uri(uriBuilder ->
        uriBuilder
          .path(
            "/repos/Corporate-Learning/cl-content-media/contents/content/video/{fileName}"
          )
          .queryParam("ref", "prod-release")
          .build(fileName)
      )
      .retrieve()
      .bodyToMono(byte[].class);

    // Fetch last commit for this file
    Mono<GitHubCommitResponse> commitMono = webClient
      .get()
      .uri(uriBuilder ->
        uriBuilder
          .path("/repos/Corporate-Learning/cl-content-media/commits")
          .queryParam("path", filePath)
          .queryParam("sha", "prod-release")
          .queryParam("per_page", 1)
          .build()
      )
      .retrieve()
      .bodyToFlux(GitHubCommitResponse.class)
      .next();

    // Combine both results
    return Mono.zip(contentMono, commitMono)
      .map(tuple -> {
        byte[] content = tuple.getT1();
        GitHubCommitResponse commit = tuple.getT2();

        return new GitHubFileWithMetadata(
          content,
          commit.getCommit().getCommitter().getDate(),
          commit.getSha(),
          commit.getCommit().getMessage()
        );
      })
      .doOnSuccess(metadata ->
        log.info(
          "Downloaded file {} with last commit date: {}",
          fileName,
          metadata.getLastCommitDate()
        )
      )
      .doOnError(error ->
        log.error("Error downloading file {} with metadata", fileName, error)
      );
  }
}
