package com.hbr.cashmere.transfer_service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvUtilTest {

  @Test
  void toHbrArticleUrlReplacesWebsiteArticleContentPrefix() {
    String articlePath = "website/article-content/2012/11/its-not-just-semantics-managing-outcomes";

    String result = CsvUtil.toAvailabilityId(articlePath);

    assertEquals(
      "https://hbr.org/2012/11/its-not-just-semantics-managing-outcomes",
      result
    );
  }

  @Test
  void toHbrArticleUrlKeepsRelativePathWhenPrefixDoesNotExist() {
    String result = CsvUtil.toAvailabilityId("2012/11/its-not-just-semantics-managing-outcomes");

    assertEquals(
      "https://hbr.org/2012/11/its-not-just-semantics-managing-outcomes",
      result
    );
  }
}

