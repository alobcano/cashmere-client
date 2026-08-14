package com.hbr.cashmere.transfer_service.constants;

import com.hbr.cashmere.transfer_service.configuration.CollectionIdProperties;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CollectionConstants {

  private static CollectionIdProperties properties;

  public CollectionConstants(CollectionIdProperties properties) {
    CollectionConstants.properties = properties;
  }

  public static int getCLArticlesBaseId() {
    return properties.getArticlesBase();
  }

  public static int getCLArticlesDeiId() {
    return properties.getArticlesDei();
  }

  public static int getCLVideosBaseId() {
    return properties.getVideosBase();
  }

  public static int getCLVideosDeiId() {
    return properties.getVideosDei();
  }

  public static int getCLPodcastsBaseId() {
    return properties.getPodcastsBase();
  }

  public static int getCLPodcastsDeiId() {
    return properties.getPodcastsDei();
  }

  public static Map<String, Integer> getCollectionNameToId() {
    return properties.getCollectionNameToId();
  }

  public static Map<Integer, String> getCollectionIdToName() {
    return properties.getCollectionIdToName();
  }
}
