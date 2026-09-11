package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.constants.CsvConstants;
import com.hbr.cashmere.transfer_service.constants.ErrorConstants;
import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import com.hbr.cashmere.transfer_service.model.OmnipubsInCollection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class CashmereService {

  private static final int UPDATE_OMNIPUB_MAX_RETRY_ATTEMPTS = 3;
  private static final Duration UPDATE_OMNIPUB_RETRY_MIN_BACKOFF = Duration.ofMillis(500);

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
    String boundary = "----CashmereBoundary" + UUID.randomUUID().toString().replace("-", "");
    byte[] multipartBody;
    try {
      multipartBody = buildMultipartBody(formData, boundary);
    } catch (IOException e) {
      return Mono.error(new RuntimeException("Failed to build createOmnipub multipart payload", e));
    }

    return webClient
        .post()
        .uri("/omnipub")
        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(multipartBody.length))
        .bodyValue(multipartBody)
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

  private byte[] buildMultipartBody(MultiValueMap<String, HttpEntity<?>> formData, String boundary)
      throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String separator = "--" + boundary + "\r\n";
    ObjectMapper objectMapper = new ObjectMapper();

    for (Map.Entry<String, List<HttpEntity<?>>> entry : formData.entrySet()) {
      String name = entry.getKey();
      List<HttpEntity<?>> values = entry.getValue();
      if (values == null) {
        continue;
      }

      for (HttpEntity<?> entity : values) {
        Object body = entity != null ? entity.getBody() : null;
        if (body == null) {
          continue;
        }

        HttpHeaders headers = entity != null ? entity.getHeaders() : HttpHeaders.EMPTY;
        String contentType =
            headers.getContentType() != null
                ? headers.getContentType().toString()
                : inferContentType(body);

        writeAscii(out, separator);
        if (body instanceof Resource resource && resource.getFilename() != null) {
          writeAscii(
              out,
              "Content-Disposition: form-data; name=\""
                  + name
                  + "\"; filename=\""
                  + resource.getFilename()
                  + "\"\r\n");
        } else {
          writeAscii(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n");
        }
        writeAscii(out, "Content-Type: " + contentType + "\r\n\r\n");

        if (body instanceof Resource resource) {
          out.write(resource.getInputStream().readAllBytes());
        } else if (body instanceof byte[] bytes) {
          out.write(bytes);
        } else if (body instanceof Map && contentType.contains("application/json")) {
          // Serialize Map to JSON string
          String jsonString = objectMapper.writeValueAsString(body);
          out.write(jsonString.getBytes(StandardCharsets.UTF_8));
        } else {
          out.write(String.valueOf(body).getBytes(StandardCharsets.UTF_8));
        }
        writeAscii(out, "\r\n");
      }
    }

    writeAscii(out, "--" + boundary + "--\r\n");
    return out.toByteArray();
  }

  private String inferContentType(Object body) {
    if (body instanceof Resource) {
      return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    if (body instanceof Map) {
      return MediaType.APPLICATION_JSON_VALUE;
    }
    return MediaType.TEXT_PLAIN_VALUE;
  }

  private void writeAscii(ByteArrayOutputStream out, String value) {
    out.writeBytes(value.getBytes(StandardCharsets.UTF_8));
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
        .bodyToMono(String.class)
        .retryWhen(
            Retry.backoff(UPDATE_OMNIPUB_MAX_RETRY_ATTEMPTS, UPDATE_OMNIPUB_RETRY_MIN_BACKOFF)
                .filter(
                    throwable ->
                        throwable instanceof RuntimeException
                            && throwable.getMessage() != null
                            && throwable.getMessage().startsWith(ErrorConstants.SERVER_ERROR))
                .doBeforeRetry(
                    signal ->
                        log.warn(
                            "Retrying updateOmnipub for Cashmere UUID: {} after server error,"
                                + " attempt: {}",
                            cashmereUuid,
                            signal.totalRetries() + 1))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
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

  public Mono<JsonNode> getOmnipubStatus(String omnipubUuid) {
    return webClient
        .get()
        .uri("/omnipub/{omnipubUuid}/status", omnipubUuid)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .doOnSuccess(
            response -> {
              if (response != null) {
                log.info("Successfully retrieved Omnipub status for omnipubUuid: {}", omnipubUuid);
              } else {
                log.info("No Omnipub status found for omnipubUuid: {}.", omnipubUuid);
              }
            })
        .doOnError(error -> log.error(ErrorConstants.CLIENT_ERROR_BODY, error.getMessage()));
  }
}
