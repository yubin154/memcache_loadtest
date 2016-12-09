package com.google.appengine.api.memcache.transcoders;

/**
 * 
 */
public class TranscoderException extends RuntimeException {

  public TranscoderException() {
    super();
  }

  public TranscoderException(String message) {
    super(message);
  }

  public TranscoderException(String message, Throwable cause) {
    super(message, cause);
  }

  public TranscoderException(Throwable cause) {
    super(cause);
  }
}
