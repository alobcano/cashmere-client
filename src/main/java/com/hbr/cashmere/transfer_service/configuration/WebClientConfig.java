package com.hbr.cashmere.transfer_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Value("${cashmere.api.token}")
  private String token;

  @Value("${cashmere.api.base-url}")
  private String cashmereBaseUrl;

  @Value("${content.api.base-url}")
  private String contentBaseUrl;

  @Value("${github.api.base-url}")
  private String githubBaseUrl;

  @Value("${github.api.token}")
  private String githubToken;

  @Bean
  WebClient cashmereWebClient() {
    return WebClient.builder()
      .baseUrl(cashmereBaseUrl)
      .defaultHeaders(headers -> headers.setBearerAuth(token))
      .build();
  }

  @Bean
  WebClient contentWebClient() {
    return WebClient.builder().baseUrl(contentBaseUrl).build();
  }

  @Bean
  WebClient githubWebClient() {
    return WebClient.builder()
      .baseUrl(githubBaseUrl)
      .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + githubToken)
      .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3.raw")
      .build();
  }
}
