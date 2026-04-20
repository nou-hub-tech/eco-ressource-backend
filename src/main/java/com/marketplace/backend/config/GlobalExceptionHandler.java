package com.marketplace.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles @Valid bean-validation failures and returns a JSON list of field errors
   * that the Angular frontend can parse.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<List<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException ex) {

    List<Map<String, String>> errors =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error -> {
                  Map<String, String> e = new HashMap<>();
                  if (error instanceof FieldError fe) {
                    e.put("field", fe.getField());
                    e.put("message", fe.getDefaultMessage());
                  } else {
                    e.put("field", error.getObjectName());
                    e.put("message", error.getDefaultMessage());
                  }
                  return e;
                })
            .collect(Collectors.toList());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  /**
   * Handles application-level IllegalArgumentException (e.g., not found, duplicate).
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, String> body = new HashMap<>();
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
