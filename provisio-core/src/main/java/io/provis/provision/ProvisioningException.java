package io.provis.provision;

public class ProvisioningException extends RuntimeException {

  private static final long serialVersionUID = -2662475912560280300L;

  public ProvisioningException(String message) {
    super(message);
  }

  public ProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProvisioningException(Throwable cause) {
    super(cause);
  }

}
