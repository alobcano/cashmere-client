package com.hbr.cashmere.transfer_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Value("${cashmere.api.token}")
  private String token;

  @Value("${cashmere.api.base-url}")
  private String baseUrl;

  @Bean
  WebClient webClient() {
    return WebClient.builder()
      .baseUrl(baseUrl)
      .defaultHeaders(headers -> headers.setBearerAuth(token))
      .build();
  }
}
