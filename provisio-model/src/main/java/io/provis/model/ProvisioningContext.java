package io.provis.model;

import java.util.Map;

public class ProvisioningContext {   

  private final ProvisioningRequest request;
  private final ProvisioningResult result;

  public ProvisioningContext(ProvisioningRequest request, ProvisioningResult result) {
    this.request = request;
    this.result = result;
  }

  public ProvisioningRequest getRequest() {
    return request;
  }
  
  public ProvisioningResult getResult() {
    return result;
  }
  
  public Map<String, String> getVariables() {
    return request.getVariables();
  }
}
