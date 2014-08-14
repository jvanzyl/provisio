package io.provis.model;

public interface ProvisioningAction {
  void execute(ProvisioningContext context) throws Exception;
}
