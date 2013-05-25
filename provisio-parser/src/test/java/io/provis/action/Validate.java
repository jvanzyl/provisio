package io.provis.action;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import javax.inject.Named;

@Named("validate")
public class Validate implements Action {

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
