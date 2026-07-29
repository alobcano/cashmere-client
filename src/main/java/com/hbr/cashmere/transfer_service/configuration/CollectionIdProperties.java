package com.hbr.cashmere.transfer_service.configuration;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "collections")
@Getter
@Setter
public class CollectionIdProperties {

  private int articlesBase;
  private int articlesDei;
  private int videosBase;
  private int videosDei;
  private int podcastsBase;
  private int podcastsDei;

  public Map<String, Integer> getCollectionNameToId() {
    return Map.of(
      "CL-Articles-Base", articlesBase,
      "CL-Articles-Inclusive", articlesDei,
      "CL-Videos-Base", videosBase,
      "CL-Videos-Inclusive", videosDei,
      "CL-Podcasts-Base", podcastsBase,
      "CL-Podcasts-Inclusive", podcastsDei
    );
  }

  public Map<Integer, String> getCollectionIdToName() {
    return Map.of(
      articlesBase, "CL-Articles-Base",
      articlesDei, "CL-Articles-Inclusive",
      videosBase, "CL-Videos-Base",
      videosDei, "CL-Videos-Inclusive",
      podcastsBase, "CL-Podcasts-Base",
      podcastsDei, "CL-Podcasts-Inclusive"
    );
  }
}

