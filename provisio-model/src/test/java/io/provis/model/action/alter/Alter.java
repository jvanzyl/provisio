package io.provis.model.action.alter;

import java.util.List;

import com.google.common.collect.Lists;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

public class Alter implements ProvisioningAction {

  private List<Insert> inserts = Lists.newArrayList();

  public List<Insert> getInserts() {
    return inserts;
  }

  public void setInserts(List<Insert> inserts) {
    this.inserts = inserts;
  }

  @Override
  public void execute(ProvisioningContext context) throws Exception {
  }
}
