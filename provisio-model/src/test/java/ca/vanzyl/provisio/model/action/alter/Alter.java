package ca.vanzyl.provisio.model.action.alter;

import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.util.ArrayList;
import java.util.List;

public class Alter implements ProvisioningAction {

    private List<Insert> inserts = new ArrayList<>();
    private List<Delete> deletes = new ArrayList<>();

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
    public void execute(ProvisioningContext context) throws Exception {}
}
