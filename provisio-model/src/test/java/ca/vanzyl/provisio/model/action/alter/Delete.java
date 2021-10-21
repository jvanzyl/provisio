package ca.vanzyl.provisio.model.action.alter;

import java.util.ArrayList;
import java.util.List;

import ca.vanzyl.provisio.model.File;

public class Delete {

  private List<File> files = new ArrayList<>();

  public List<File> getFiles() {
    return files;
  }
}
