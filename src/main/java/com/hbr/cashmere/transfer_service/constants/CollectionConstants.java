package com.hbr.cashmere.transfer_service.constants;

import java.util.Map;

public class CollectionConstants {

  private CollectionConstants() {}

  public static final String CL_ARTICLES_BASE = "CL-Articles-Base";
  public static final String CL_ARTICLES_DEI = "CL-Articles-Inclusive";
  public static final String CL_VIDEOS_BASE = "CL-Videos-Base";
  public static final String CL_VIDEOS_DEI = "CL-Videos-Inclusive";
  public static final String CL_PODCASTS_BASE = "CL-Podcasts-Base";
  public static final String CL_PODCASTS_DEI = "CL-Podcasts-Inclusive";

  public static final int CL_ARTICLES_BASE_ID = 451;
  public static final int CL_ARTICLES_DEI_ID = 452;
  public static final int CL_VIDEOS_BASE_ID = 453;
  public static final int CL_VIDEOS_DEI_ID = 454;
  public static final int CL_PODCASTS_BASE_ID = 455;
  public static final int CL_PODCASTS_DEI_ID = 456;

  public static final Map<String, Integer> COLLECTION_NAME_TO_ID = Map.of(
    CL_ARTICLES_BASE, CL_ARTICLES_BASE_ID,
    CL_ARTICLES_DEI, CL_ARTICLES_DEI_ID,
    CL_VIDEOS_BASE, CL_VIDEOS_BASE_ID,
    CL_VIDEOS_DEI, CL_VIDEOS_DEI_ID,
    CL_PODCASTS_BASE, CL_PODCASTS_BASE_ID,
    CL_PODCASTS_DEI, CL_PODCASTS_DEI_ID
  );

  public static final Map<Integer, String> COLLECTION_ID_TO_NAME = Map.of(
    CL_ARTICLES_BASE_ID, CL_ARTICLES_BASE,
    CL_ARTICLES_DEI_ID, CL_ARTICLES_DEI,
    CL_VIDEOS_BASE_ID, CL_VIDEOS_BASE,
    CL_VIDEOS_DEI_ID, CL_VIDEOS_DEI,
    CL_PODCASTS_BASE_ID, CL_PODCASTS_BASE,
    CL_PODCASTS_DEI_ID, CL_PODCASTS_DEI
  );
}
