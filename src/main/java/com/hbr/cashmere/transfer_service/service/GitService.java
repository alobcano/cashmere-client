package com.hbr.cashmere.transfer_service.service;

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
}
