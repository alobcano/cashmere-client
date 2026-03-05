package com.hbr.cashmere.transfer_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
public class ContentService {

  private final WebClient webClient;

  public ContentService(@Qualifier("contentWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<JsonNode> fetchMetadata(String contentId) {
    return webClient
      .get()
      .uri("/metadata/{contentId}", contentId)
      .retrieve()
      .bodyToMono(JsonNode.class)
      .doOnSuccess(metadata ->
        log.info("Fetched metadata for content ID {}: {}", contentId, metadata)
      )
      .doOnError(error ->
        log.error(
          "Error fetching metadata for content ID {}: {}",
          contentId,
          error.getMessage()
        )
      );
  }
}
