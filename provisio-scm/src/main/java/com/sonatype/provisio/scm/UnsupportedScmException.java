package com.sonatype.provisio.scm;

public class UnsupportedScmException extends UnsupportedOperationException {

  private static final long serialVersionUID = -8771292432411293009L;

  public UnsupportedScmException(String msg) {
    super(msg);
  }

  public UnsupportedScmException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public UnsupportedScmException(Throwable cause) {
    super(cause);
  }

}
