package io.provis.action;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioContext;

import javax.inject.Named;

@Named("validate")
public class Validate implements ProvisioningAction {

  private boolean validate;
  
  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  public boolean isValidate() {
    return validate;
  }

  public void setValidate(boolean validate) {
    this.validate = validate;
  }
  
  
}
