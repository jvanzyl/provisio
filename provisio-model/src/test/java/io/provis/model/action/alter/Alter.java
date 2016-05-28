package io.provis.model.action.alter;

import java.util.List;

import com.google.common.collect.Lists;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

public class Alter implements ProvisioningAction {

  private List<Insert> inserts = Lists.newArrayList();
  private List<Delete> deletes = Lists.newArrayList();

  public List<Insert> getInserts() {
    return inserts;
  }

  public void setInserts(List<Insert> inserts) {
    this.inserts = inserts;
  }

  public List<Delete> getDeletes() {
    return deletes;
  }

  public void setDeletes(List<Delete> deletes) {
    this.deletes = deletes;
  }

  @Override
  public void execute(ProvisioningContext context) throws Exception {
  }
}
