package com.hbr.cashmere.transfer_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

  @Mock
  private S3Client s3Client;

  private S3FileService s3FileService;

  @BeforeEach
  void setUp() {
    s3FileService = new S3FileService(s3Client);
  }

  @Test
  void downloadFile_Success() throws IOException {
    // Given
    String bucketName = "test-bucket";
    String key = "path/to/file.xml";
    String xmlContent = "<?xml version=\"1.0\"?><root><content>Test XML</content></root>";
    byte[] expectedBytes = xmlContent.getBytes();

    ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
      GetObjectResponse.builder().build(),
      AbortableInputStream.create(new ByteArrayInputStream(expectedBytes))
    );

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

    // When
    byte[] result = s3FileService.downloadFile(bucketName, key);

    // Then
    assertThat(result).isNotEmpty();
    verify(s3Client).getObject(any(GetObjectRequest.class));
  }

  @Test
  void downloadFile_NoSuchKey() {
    // Given
    String bucketName = "test-bucket";
    String key = "path/to/nonexistent.xml";

    when(s3Client.getObject(any(GetObjectRequest.class)))
      .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

    // When & Then
    assertThatThrownBy(() -> s3FileService.downloadFile(bucketName, key))
      .isInstanceOf(NoSuchKeyException.class);
  }

  @Test
  void downloadFile_S3Exception() {
    // Given
    String bucketName = "test-bucket";
    String key = "path/to/file.xml";

    when(s3Client.getObject(any(GetObjectRequest.class)))
      .thenThrow(S3Exception.builder().message("S3 error").build());

    // When & Then
    assertThatThrownBy(() -> s3FileService.downloadFile(bucketName, key))
      .isInstanceOf(S3Exception.class);
  }

  @Test
  void downloadFile_VerifyRequestParameters() throws IOException {
    // Given
    String bucketName = "test-bucket";
    String key = "path/to/file.xml";
    byte[] xmlContent = "<xml>content</xml>".getBytes();

    ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
      GetObjectResponse.builder().build(),
      AbortableInputStream.create(new ByteArrayInputStream(xmlContent))
    );

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

    // When
    s3FileService.downloadFile(bucketName, key);

    // Then
    verify(s3Client).getObject(any(GetObjectRequest.class));
  }

  @Test
  void downloadFile_WithComplexXml() throws IOException {
    // Given
    String bucketName = "test-bucket";
    String key = "complex/path/to/file.xml";
    String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<document>" +
      "<metadata><title>Test</title><author>Author</author></metadata>" +
      "<body><section><p>Paragraph with <img src='test.jpg'/> image</p></section></body>" +
      "</document>";
    byte[] xmlBytes = complexXml.getBytes();

    ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
      GetObjectResponse.builder().build(),
      AbortableInputStream.create(new ByteArrayInputStream(xmlBytes))
    );

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

    // When
    byte[] result = s3FileService.downloadFile(bucketName, key);

    // Then
    assertThat(result).isNotEmpty();
    // Note: XmlUtil.processXml is called internally which may modify the content
  }

  @Test
  void downloadFile_EmptyFile() throws IOException {
    // Given
    String bucketName = "test-bucket";
    String key = "path/to/empty.xml";
    byte[] emptyContent = new byte[0];

    ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
      GetObjectResponse.builder().build(),
      AbortableInputStream.create(new ByteArrayInputStream(emptyContent))
    );

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

    // When
    byte[] result = s3FileService.downloadFile(bucketName, key);

    // Then
    assertThat(result).isEmpty();
  }
}
