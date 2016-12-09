package com.google.cloud.cache.apps.loadtest;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

final class ResponseWriter {
  static ResponseWriter create(HttpServletResponse response) {
    response.setContentType("text/plain");
    return new ResponseWriter(response);
  }

  private final HttpServletResponse response;

  private ResponseWriter(HttpServletResponse response) {
    this.response = response;
  }

  void write(String text) throws IOException {
    response.getWriter().print(text);
  }

  void write(String key, String value) throws IOException {
    response.getWriter().printf("%s: %s\n", key, value);
  }

  void flush() throws IOException {
    response.getWriter().flush();
  }

  void fail() {
    response.setStatus(507);
  }
}
