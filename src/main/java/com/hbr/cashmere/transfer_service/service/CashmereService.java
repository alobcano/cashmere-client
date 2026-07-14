package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.constants.CsvConstants;
import com.hbr.cashmere.transfer_service.constants.ErrorConstants;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubsInCollection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
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
   *
   * @param formData The form data to send in the request
   * @return A Mono emitting the response body as a String
   */
  public Mono<String> createOmnipub(MultiValueMap<String, HttpEntity<?>> formData) {
    logCreateOmnipubPayload(formData);
    logCreateOmnipubRequestBody(formData);
    return webClient
        .post()
        .uri("/omnipub")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(formData)
        .exchangeToMono(
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          if (response.statusCode().is2xxSuccessful()) {
                            log.info(
                                "Omnipub created successfully, External ID: {}, Success response"
                                    + " body: {}",
                                formData.getFirst("external_id"),
                                body);
                            return Mono.just(body);
                          }
                          if (response.statusCode().is4xxClientError()) {
                            log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
                            return Mono.error(
                                new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
                          }
                          if (response.statusCode().is5xxServerError()) {
                            log.error(ErrorConstants.SERVER_ERROR_BODY, body);
                            return Mono.error(
                                new RuntimeException(ErrorConstants.SERVER_ERROR + body));
                          }
                          return Mono.error(
                              new RuntimeException(
                                  "Unexpected status from Cashmere createOmnipub: "
                                      + response.statusCode()
                                      + " body: "
                                      + body));
                        }));
  }

  private void logCreateOmnipubPayload(MultiValueMap<String, HttpEntity<?>> formData) {
    Set<String> contentFields = Set.of("file", "file_url", "html_content", "md_content");

    var presentContentFields =
        contentFields.stream()
            .filter(
                key ->
                    formData.containsKey(key)
                        && formData.get(key) != null
                        && !formData.get(key).isEmpty())
            .toList();

    log.info("createOmnipub payload keys: {}", formData.keySet());
    log.info(
        "createOmnipub content fields present: {} (count={})",
        presentContentFields,
        presentContentFields.size());

    if (presentContentFields.size() != 1) {
      log.warn(
          "Expected exactly one of uploaded_file, file_url, html_content, md_content. Found: {}",
          presentContentFields);
    }
  }

  private void logCreateOmnipubRequestBody(MultiValueMap<String, HttpEntity<?>> formData) {
    StringBuilder bodySnapshot = new StringBuilder("{\n");

    for (Map.Entry<String, List<HttpEntity<?>>> entry : formData.entrySet()) {
      bodySnapshot.append("  \"").append(entry.getKey()).append("\": [\n");

      List<HttpEntity<?>> values = entry.getValue();
      if (values != null) {
        for (HttpEntity<?> value : values) {
          Object partBody = value != null ? value.getBody() : null;
          String bodyType = partBody != null ? partBody.getClass().getName() : "null";
          String bodyPreview = partBody != null ? String.valueOf(partBody) : "null";

          if (bodyPreview.length() > 500) {
            bodyPreview = bodyPreview.substring(0, 500) + "...<truncated>";
          }

          bodySnapshot
              .append("    { headers: ")
              .append(value != null ? value.getHeaders() : "null")
              .append(", bodyType: ")
              .append(bodyType)
              .append(", bodyPreview: ")
              .append(bodyPreview)
              .append(" },\n");
        }
      }

      bodySnapshot.append("  ],\n");
    }

    bodySnapshot.append("}");
    log.info("createOmnipub full multipart payload snapshot:\n{}", bodySnapshot);
  }

  /**
   * Deletes an Omnipub by sending a DELETE request to the Cashmere API with the provided Cashmere
   * UUID.
   *
   * @param cashmereUuid The Cashmere UUID of the Omnipub to delete
   * @return A Mono emitting the response body as a String
   */
  public Mono<String> deleteOmnipub(String cashmereUuid) {
    return webClient
        .delete()
        .uri("/omnipub/{cashmereUuid}/deprecate", cashmereUuid)
        .retrieve()
        .onStatus(
            HttpStatusCode::is2xxSuccessful,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.info(
                              "Omnipub deleted successfully, Cashmere UUID: {}, Success response"
                                  + " body: {}",
                              cashmereUuid,
                              body);
                          return Mono.empty();
                        }))
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.SERVER_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.SERVER_ERROR + body));
                        }))
        .bodyToMono(String.class);
  }

  /**
   * Retrieves Omnipubs by sending a GET request to the Cashmere API with the provided external ID.
   *
   * @param externalId The external ID to filter Omnipubs
   * @return A Mono emitting the response body as a JsonNode
   */
  public Mono<JsonNode> getOmnipubs(String externalId) {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path("/omnipubs").queryParam("external_id", externalId).build())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .doOnSuccess(
            response -> {
              if (response != null
                  && response.has(CsvConstants.ITEMS)
                  && response.get(CsvConstants.ITEMS).isArray()
                  && !response.get(CsvConstants.ITEMS).isEmpty()) {
                log.info("Successfully retrieved Omnipubs for external_id: {}", externalId);
              } else {
                log.info("No Omnipubs found for external_id: {}.", externalId);
              }
            })
        .doOnError(error -> log.error(ErrorConstants.CLIENT_ERROR_BODY, error.getMessage()));
  }

  /**
   * Retrieves Omnipubs by sending a GET request to the Cashmere API with the provided external ID
   * and collection name.
   *
   * @param externalId The external ID to filter Omnipubs
   * @param collection The collection name to filter Omnipubs
   * @return A Mono emitting the response body as a JsonNode
   */
  public Mono<JsonNode> getOmnipubs(String externalId, Integer collection) {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/omnipubs")
                    .queryParam("external_id", externalId)
                    .queryParam("collection", collection)
                    .build())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .doOnSuccess(
            response -> {
              if (response != null
                  && response.has(CsvConstants.ITEMS)
                  && response.get(CsvConstants.ITEMS).isArray()
                  && !response.get(CsvConstants.ITEMS).isEmpty()) {
                log.info(
                    "Successfully retrieved Omnipubs for external_id: {} and collection: {}",
                    externalId,
                    collection);
              } else {
                log.info(
                    "No Omnipubs found for external_id: {} and collection: {}.",
                    externalId,
                    collection);
              }
            })
        .doOnError(error -> log.error(ErrorConstants.CLIENT_ERROR_BODY, error.getMessage()));
  }

  /**
   * Updates an Omnipub's metadata by sending a PUT request to the Cashmere API with the provided
   * Cashmere UUID and metadata.
   *
   * @param cashmereUuid The UUID of the Omnipub in Cashmere
   * @param metadata The metadata to update
   * @return A Mono emitting the response body as a String
   */
  public Mono<String> updateOmnipub(String cashmereUuid, OmnipubMetadata metadata) {
    return webClient
        .put()
        .uri("/omnipub/{cashmereUuid}/metadata", cashmereUuid)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(metadata)
        .retrieve()
        .onStatus(
            HttpStatusCode::is2xxSuccessful,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.info(
                              "Omnipub updated successfully, Cashmere UUID: {}, Success response"
                                  + " body: {}",
                              cashmereUuid,
                              body);
                          return Mono.empty();
                        }))
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.SERVER_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.SERVER_ERROR + body));
                        }))
        .bodyToMono(String.class);
  }

  public Mono<String> removeOmnipubFromCollection(
      OmnipubsInCollection cashmereUuid, int collectionId) {
    return webClient
        .post()
        .uri("/collections/{collection_id}/remove", collectionId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(cashmereUuid)
        .retrieve()
        .onStatus(
            HttpStatusCode::is2xxSuccessful,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.info(
                              "Omnipub removed from collection successfully, Cashmere UUIDs: {},"
                                  + " Collection ID: {}",
                              cashmereUuid.getPublicationUuids(),
                              collectionId);
                          return Mono.empty();
                        }))
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.SERVER_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.SERVER_ERROR + body));
                        }))
        .bodyToMono(String.class);
  }

  public Mono<String> addOmnipubToCollection(OmnipubsInCollection cashmereUuid, int collectionId) {
    return webClient
        .post()
        .uri("/collections/{collection_id}/add", collectionId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(cashmereUuid)
        .retrieve()
        .onStatus(
            HttpStatusCode::is2xxSuccessful,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.info(
                              "Omnipub added to collection successfully, Cashmere UUIDs: {},"
                                  + " Collection ID: {}",
                              cashmereUuid.getPublicationUuids(),
                              collectionId);
                          return Mono.empty();
                        }))
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.CLIENT_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.CLIENT_ERROR + body));
                        }))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                response
                    .bodyToMono(String.class)
                    .flatMap(
                        body -> {
                          log.error(ErrorConstants.SERVER_ERROR_BODY, body);
                          return Mono.error(
                              new RuntimeException(ErrorConstants.SERVER_ERROR + body));
                        }))
        .bodyToMono(String.class);
  }

  public Mono<JsonNode> getOmnipub(String omnipubUuid) {
    return webClient
        .get()
        .uri("/omnipub/{omnipubUuid}", omnipubUuid)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .doOnSuccess(
            response -> {
              if (response != null) {
                log.info("Successfully retrieved Omnipub for omnipubUuid: {}", omnipubUuid);
              } else {
                log.info("No Omnipub found for omnipubUuid: {}.", omnipubUuid);
              }
            })
        .doOnError(error -> log.error(ErrorConstants.CLIENT_ERROR_BODY, error.getMessage()));
  }
}
