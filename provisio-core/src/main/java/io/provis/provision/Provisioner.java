package io.provis.provision;

import io.tesla.aether.TeslaAether;

public interface Provisioner {
  ProvisioningResult provision(ProvisioningRequest request);
  void setAether(TeslaAether aether);
  TeslaAether getAether();
}
