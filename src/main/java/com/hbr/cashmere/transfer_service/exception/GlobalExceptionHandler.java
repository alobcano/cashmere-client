package com.hbr.cashmere.transfer_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<String> handleMaxUploadSizeExceededException(
    MaxUploadSizeExceededException e
  ) {
    return ResponseEntity
      .status(413)
      .body(
        "File size exceeds the maximum allowed limit of 50MB. Please upload a smaller file."
      );
  }
}
