package ca.vanzyl.provisio.model.action.alter;

import ca.vanzyl.provisio.model.File;
import java.util.ArrayList;
import java.util.List;

public class Delete {

    private final List<File> files = new ArrayList<>();

    public List<File> getFiles() {
        return files;
    }
}
