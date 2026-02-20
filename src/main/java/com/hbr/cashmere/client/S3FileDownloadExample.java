package com.hbr.cashmere.client;

import com.hbr.cashmere.client.service.S3FileService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.nio.file.Paths;

@Component
public class S3FileDownloadExample implements CommandLineRunner {
  private static final Log logger = LogFactory.getLog(S3FileDownloadExample.class);  
  private final S3FileService s3FileService;
    

    public S3FileDownloadExample(S3FileService s3FileService) {
        this.s3FileService = s3FileService;
    }

    @Override
    public void run(String... args) throws Exception {
        String bucketName = "hbrg-prod";
        String key = "website/article-content/1957/09/listening-to-people.xml";
        String destination = "listening-to-people.xml";
        try {
            s3FileService.downloadAndCleanXmlFile(bucketName, key, Paths.get(destination));
            logger.info("File downloaded successfully to " + destination);
        } catch (Exception e) {
            logger.error("Failed to download file: " + e.getMessage(), e);
        }
    }
}
