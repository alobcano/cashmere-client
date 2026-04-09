package com.hbr.cashmere.transfer_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ContentServiceTest {

  private MockWebServer mockWebServer;
  private ContentService contentService;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
    contentService = new ContentService(webClient);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void fetchMetadata_Success() throws Exception {
    // Given
    String contentId = "test-content-id-123";
    String responseBody = "{" +
      "\"id\":\"" + contentId + "\"," +
      "\"title\":\"Test Content\"," +
      "\"authors\":[\"Author 1\",\"Author 2\"]," +
      "\"publisher\":\"Test Publisher\"" +
      "}";

    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json")
    );

    // When & Then
    StepVerifier
      .create(contentService.fetchMetadata(contentId))
      .assertNext(jsonNode -> {
        assertThat(jsonNode.has("id")).isTrue();
        assertThat(jsonNode.get("id").asString()).isEqualTo(contentId);
        assertThat(jsonNode.has("title")).isTrue();
        assertThat(jsonNode.get("title").asString()).isEqualTo("Test Content");
        assertThat(jsonNode.has("authors")).isTrue();
        assertThat(jsonNode.get("authors").isArray()).isTrue();
        assertThat(jsonNode.get("authors").size()).isEqualTo(2);
      })
      .verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/metadata/" + contentId);
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void fetchMetadata_NotFound() {
    // Given
    String contentId = "non-existent-id";
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(404)
        .setBody("{\"error\":\"Content not found\"}")
        .addHeader("Content-Type", "application/json")
    );

    // When & Then
    StepVerifier
      .create(contentService.fetchMetadata(contentId))
      .expectError()
      .verify();
  }

  @Test
  void fetchMetadata_ServerError() {
    // Given
    String contentId = "test-content-id";
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(500)
        .setBody("{\"error\":\"Internal server error\"}")
        .addHeader("Content-Type", "application/json")
    );

    // When & Then
    StepVerifier
      .create(contentService.fetchMetadata(contentId))
      .expectError()
      .verify();
  }

  @Test
  void fetchMetadata_EmptyResponse() {
    // Given
    String contentId = "test-content-id";
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(200)
        .setBody("{}")
        .addHeader("Content-Type", "application/json")
    );

    // When & Then
    StepVerifier
      .create(contentService.fetchMetadata(contentId))
      .assertNext(jsonNode -> {
        assertThat(jsonNode.isEmpty()).isTrue();
      })
      .verifyComplete();
  }

  @Test
  void fetchMetadata_ComplexMetadata() {
    // Given
    String contentId = "complex-content-id";
    String responseBody = "{" +
      "\"id\":\"" + contentId + "\"," +
      "\"title\":\"Complex Content\"," +
      "\"authors\":[\"Author 1\",\"Author 2\",\"Author 3\"]," +
      "\"publisher\":\"HBR Publishing\"," +
      "\"publicationDate\":\"2024-01-01\"," +
      "\"tags\":[\"business\",\"management\",\"leadership\"]," +
      "\"metadata\":{" +
      "\"category\":\"Article\"," +
      "\"subcategory\":\"Leadership\"" +
      "}" +
      "}";

    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json")
    );

    // When & Then
    StepVerifier
      .create(contentService.fetchMetadata(contentId))
      .assertNext(jsonNode -> {
        assertThat(jsonNode.get("id").asString()).isEqualTo(contentId);
        assertThat(jsonNode.get("title").asString()).isEqualTo("Complex Content");
        assertThat(jsonNode.get("authors").size()).isEqualTo(3);
        assertThat(jsonNode.has("metadata")).isTrue();
        assertThat(jsonNode.get("metadata").has("category")).isTrue();
        assertThat(jsonNode.get("tags").isArray()).isTrue();
      })
      .verifyComplete();
  }
}
