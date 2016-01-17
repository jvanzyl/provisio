/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
