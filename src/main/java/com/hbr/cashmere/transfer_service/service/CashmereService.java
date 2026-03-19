package com.hbr.cashmere.transfer_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import com.hbr.cashmere.transfer_service.constants.ErrorConstants;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
public class CashmereService {

  private final WebClient webClient;

  public CashmereService(@Qualifier("cashmereWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * Creates an Omnipub by sending a POST request to the Cashmere API with the provided form data.
   * @param formData The form data to send in the request
   * @return A Mono emitting the response body as a String
   */
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
            log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
          })
      )
      .onStatus(HttpStatusCode::is5xxServerError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error(ErrorConstants.SERVER_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.SERVER_ERROR + body));
          })
      )
      .bodyToMono(String.class);
  }

  /**
   * Deletes an Omnipub by sending a DELETE request to the Cashmere API with the provided Cashmere UUID.
   * @param cashmereUuid The Cashmere UUID of the Omnipub to delete
   * @return A Mono emitting the response body as a String
   */
  public Mono<String> deleteOmnipub(String cashmereUuid) {
    return webClient
      .delete()
      .uri("/omnipub/{cashmereUuid}/deprecate", cashmereUuid)
      .retrieve()
      .onStatus(HttpStatusCode::is2xxSuccessful, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.info(
              "Omnipub deleted successfully, Cashmere UUID: {}, Success response body: {}",
              cashmereUuid,
              body
            );
            return Mono.empty();
          })
      )
      .onStatus(HttpStatusCode::is4xxClientError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
          })
      )
      .onStatus(HttpStatusCode::is5xxServerError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error(ErrorConstants.SERVER_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.SERVER_ERROR + body));
          })
      )
      .bodyToMono(String.class);
  }

  /**
   * Retrieves Omnipubs by sending a GET request to the Cashmere API with the provided external ID.
   * @param externalId The external ID to filter Omnipubs
   * @return A Mono emitting the response body as a JsonNode
   */
  public Mono<JsonNode> getOmnipubs(String externalId) {
    return webClient
      .get()
      .uri(uriBuilder ->
        uriBuilder
          .path("/omnipubs")
          .queryParam("external_id", externalId)
          .build()
      )
      .retrieve()
      .bodyToMono(JsonNode.class)
      .doOnSuccess(response ->
        log.info(
          "Successfully retrieved Omnipubs for external_id: {}.",
          externalId
        )
      )
      .doOnError(error ->
        log.error(
          ErrorConstants.CLIENT_ERROR_BODY,
          error.getMessage()
        )
      );
  }


  public Mono<String> updateOmnipub(String cashmereUuid, OmnipubMetadata metadata) {
    return webClient
      .put()
      .uri("/omnipub/{cashmereUuid}/metadata", cashmereUuid)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(metadata)
      .retrieve()
      .onStatus(HttpStatusCode::is2xxSuccessful, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.info(
              "Omnipub updated successfully, Cashmere UUID: {}, Success response body: {}",
              cashmereUuid,
              body
            );
            return Mono.empty();
          })
      )
      .onStatus(HttpStatusCode::is4xxClientError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
          })
      )
      .onStatus(HttpStatusCode::is5xxServerError, response ->
        response
          .bodyToMono(String.class)
          .flatMap(body -> {
            log.error(ErrorConstants.SERVER_ERROR_BODY, body);
            return Mono.error(new RuntimeException(ErrorConstants.SERVER_ERROR + body));
          })
      )
      .bodyToMono(String.class);
    
  }
}
