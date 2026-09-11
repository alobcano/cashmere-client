package com.hbr.cashmere.transfer_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hbr.cashmere.transfer_service.model.OmnipubMetadata;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class CashmereServiceTest {

  private MockWebServer mockWebServer;
  private CashmereService cashmereService;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
    cashmereService = new CashmereService(webClient);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void createOmnipub_Success() throws Exception {
    // Given
    String responseBody = "{\"uuid\":\"test-uuid\",\"status\":\"created\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(201)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    MultiValueMap<String, HttpEntity<?>> formData = new LinkedMultiValueMap<>();
    formData.add("external_id", new HttpEntity<>("test-external-id"));
    formData.add("collection_ids", new HttpEntity<>("123"));
    formData.add("html_content", new HttpEntity<>("<html><body>Test content</body></html>"));

    // When & Then
    StepVerifier.create(cashmereService.createOmnipub(formData))
        .assertNext(
            response -> {
              assertThat(response).contains("test-uuid");
              assertThat(response).contains("created");
            })
        .verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/omnipub");
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeader("Content-Type")).contains("multipart/form-data");
  }

  @Test
  void createOmnipub_ClientError() {
    // Given
    String errorBody = "{\"error\":\"Invalid request\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody(errorBody)
            .addHeader("Content-Type", "application/json"));

    MultiValueMap<String, HttpEntity<?>> formData = new LinkedMultiValueMap<>();
    formData.add("external_id", new HttpEntity<>("test-external-id"));

    // When & Then
    StepVerifier.create(cashmereService.createOmnipub(formData))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Client Error"))
        .verify();
  }

  @Test
  void createOmnipub_ServerError() {
    // Given
    String errorBody = "{\"error\":\"Internal server error\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody(errorBody)
            .addHeader("Content-Type", "application/json"));

    MultiValueMap<String, HttpEntity<?>> formData = new LinkedMultiValueMap<>();
    formData.add("external_id", new HttpEntity<>("test-external-id"));

    // When & Then
    StepVerifier.create(cashmereService.createOmnipub(formData))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Server Error"))
        .verify();
  }

  @Test
  void deleteOmnipub_Success() throws Exception {
    // Given
    String responseBody = "{\"status\":\"deleted\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    String cashmereUuid = "test-uuid-123";

    // When & Then
    StepVerifier.create(cashmereService.deleteOmnipub(cashmereUuid)).verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/omnipub/" + cashmereUuid + "/deprecate");
    assertThat(request.getMethod()).isEqualTo("DELETE");
  }

  @Test
  void deleteOmnipub_ClientError() {
    // Given
    String errorBody = "{\"error\":\"Not found\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody(errorBody)
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(cashmereService.deleteOmnipub("non-existent-uuid"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Client Error"))
        .verify();
  }

  @Test
  void getOmnipubs_Success() throws Exception {
    // Given
    String responseBody = "{\"items\":[{\"uuid\":\"uuid1\",\"external_id\":\"ext1\"}],\"total\":1}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    String externalId = "test-external-id";

    // When & Then
    StepVerifier.create(cashmereService.getOmnipubs(externalId))
        .assertNext(
            jsonNode -> {
              assertThat(jsonNode.has("items")).isTrue();
              assertThat(jsonNode.get("items").isArray()).isTrue();
              assertThat(jsonNode.get("items").size()).isEqualTo(1);
            })
        .verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/omnipubs?external_id=" + externalId);
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void getOmnipubs_EmptyResult() {
    // Given
    String responseBody = "{\"items\":[],\"total\":0}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(cashmereService.getOmnipubs("non-existent-id"))
        .assertNext(
            jsonNode -> {
              assertThat(jsonNode.has("items")).isTrue();
              assertThat(jsonNode.get("items").size()).isZero();
            })
        .verifyComplete();
  }

  @Test
  void updateOmnipub_Success() throws Exception {
    // Given
    String responseBody = "{\"uuid\":\"test-uuid\",\"status\":\"updated\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(responseBody)
            .addHeader("Content-Type", "application/json"));

    String cashmereUuid = "test-uuid-123";
    OmnipubMetadata metadata =
        new OmnipubMetadata(
            "Test Title",
            new String[] {"Author 1", "Author 2"},
            "Test Publisher",
            "2024-01-01",
            "2024-01-15",
                "https://example.com/source-url");

    // When & Then
    StepVerifier.create(cashmereService.updateOmnipub(cashmereUuid, metadata)).verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/omnipub/" + cashmereUuid + "/metadata");
    assertThat(request.getMethod()).isEqualTo("PUT");
    assertThat(request.getHeader("Content-Type")).contains("application/json");
  }

  @Test
  void updateOmnipub_ClientError() {
    // Given
    String errorBody = "{\"error\":\"Invalid metadata\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody(errorBody)
            .addHeader("Content-Type", "application/json"));

    OmnipubMetadata metadata = new OmnipubMetadata("Test Title", new String[] {}, null, null ,null, null);

    // When & Then
    StepVerifier.create(cashmereService.updateOmnipub("test-uuid", metadata))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Client Error"))
        .verify();
  }

  @Test
  void updateOmnipub_ServerError() {
    // Given
    String errorBody = "{\"error\":\"Internal server error\"}";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody(errorBody)
            .addHeader("Content-Type", "application/json"));

    OmnipubMetadata metadata = new OmnipubMetadata("Test Title", new String[] {}, null, null, null, null);

    // When & Then
    StepVerifier.create(cashmereService.updateOmnipub("test-uuid", metadata))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Server Error"))
        .verify();
  }
}
