package com.hbr.cashmere.client.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import com.hbr.cashmere.client.util.XmlUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class S3FileService {

  private final S3Client s3Client;

  public S3FileService(S3Client s3Client) {
    this.s3Client = s3Client;
  }


  /**
   * Downloads an XML file from S3, removes all <img> tags, and saves the cleaned content.
   * 
   * @param bucketName The S3 bucket name
   * @param key The S3 object key (path to the XML file)
   * @param destinationPath The local path to save the cleaned file
   * @throws IOException if file operations fail
   */
  public void downloadAndCleanXmlFile(String bucketName, String key, Path destinationPath)
      throws IOException {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();
    File xmlFile = File.createTempFile("s3file", ".xml");
    try (var s3Object = s3Client.getObject(getObjectRequest)) {
      Files.write(xmlFile.toPath(), s3Object.readAllBytes());
    }
    XmlUtil.saveDocumentToFile(XmlUtil.removeImages(xmlFile), destinationPath);

    xmlFile.deleteOnExit();
  }
}
