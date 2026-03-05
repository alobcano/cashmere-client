package com.hbr.cashmere.transfer_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CashmereService {

  private final WebClient webClient;

  public CashmereService(@Qualifier("cashmereWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<String> createOmnipub(
    MultiValueMap<String, HttpEntity<?>> formData
  ) {
    return webClient
      .post()
      .uri("/omnipub")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .bodyValue(formData)
      .retrieve()
      .onStatus(HttpStatusCode::is2xxSuccessful, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.info(
              "Omnipub created successfully, External ID: {}, Success response body: {}",
              formData.getFirst("external_id"),
              body
            );
            return Mono.empty();
          })
      )
      .onStatus(HttpStatusCode::is4xxClientError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error("Client error body: {}", body);
            return Mono.error(new RuntimeException("Client Error: " + body));
          })
      )
      .onStatus(HttpStatusCode::is5xxServerError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error("Server error body: {}", body);
            return Mono.error(new RuntimeException("Server Error: " + body));
          })
      )
      .bodyToMono(String.class);
  }
}
