package com.hbr.cashmere.transfer_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.test.StepVerifier;

class GitServiceTest {

  private MockWebServer mockWebServer;
  private GitService gitService;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
    gitService = new GitService(webClient);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void downloadXml_Success() throws Exception {
    // Given
    byte[] xmlContent = "<xml>test content</xml>".getBytes();
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(200)
        .setBody(new String(xmlContent))
        .addHeader("Content-Type", "application/xml")
    );

    String fileName = "test-file.xml";

    // When & Then
    StepVerifier
      .create(gitService.downloadXml(fileName))
      .assertNext(result -> assertThat(result).isEqualTo(xmlContent))
      .verifyComplete();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).contains("/repos/Corporate-Learning/cl-content-media/contents/content/video/" + fileName);
    assertThat(request.getPath()).contains("ref=prod-release");
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void downloadXml_Error() {
    // Given
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(404)
        .setBody("{\"message\":\"Not Found\"}")
    );

    // When & Then
    StepVerifier
      .create(gitService.downloadXml("non-existent.xml"))
      .expectError()
      .verify();
  }

  @Test
  void downloadXmlWithMetadata_FileDownloadError() {
    // Given
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(404)
        .setBody("{\"message\":\"File not found\"}")
    );

    // When & Then
    StepVerifier
      .create(gitService.downloadXmlWithMetadata("non-existent.xml"))
      .expectError()
      .verify();
  }

  @Test
  void downloadXmlWithMetadata_CommitFetchError() {
    // Given
    byte[] xmlContent = "<xml>test content</xml>".getBytes();
    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(200)
        .setBody(new String(xmlContent))
    );

    mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(500)
        .setBody("{\"message\":\"Internal server error\"}")
    );

    // When & Then
    StepVerifier
      .create(gitService.downloadXmlWithMetadata("test-file.xml"))
      .expectError()
      .verify();
  }
}
