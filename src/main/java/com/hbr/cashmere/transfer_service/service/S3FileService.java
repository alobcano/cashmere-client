package com.hbr.cashmere.transfer_service.service;

import com.hbr.cashmere.transfer_service.util.XmlUtil;
import java.io.IOException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class S3FileService {

  private final S3Client s3Client;

  public S3FileService(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  /**
   * Downloads a file from S3 and processes the XML content to remove images and clean HTML.
   *
   * @param bucketName The S3 bucket name
   * @param key The S3 object key (path to the XML file)
   * @return The cleaned XML content as a byte array
   * @throws IOException if file operations fail
   */
  public byte[] downloadFile(String bucketName, String key) throws IOException {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .build();
    try (var s3Object = s3Client.getObject(getObjectRequest)) {
      return XmlUtil.processXml(s3Object.readAllBytes());
    }
  }
}
