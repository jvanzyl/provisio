package io.provis.provision;

import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;

public interface MavenProvisioner {
  ProvisioningResult provision(ProvisioningRequest request);
}
