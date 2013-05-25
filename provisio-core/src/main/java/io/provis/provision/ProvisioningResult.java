package io.provis.provision;

import io.provis.model.ProvisioModel;

// The provisioning result should carry with it:
// what artifacts have been resolved and where they live
// configuration files and what is contained in them

// mvn tesla:awesome
//   provision tesla and record the result
// be able to roll back and forward between versions
// be able to see the change reason, so the linking of the issues
// allow for easy submission of problems

public class ProvisioningResult {
  private ProvisioModel assembly;

  public ProvisioningResult(ProvisioModel assembly) {
    this.assembly = assembly;
  }

  public ProvisioModel getAssembly() {
    return assembly;
  }
}
