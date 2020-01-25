package ca.vanzyl.provisio.model.action.alter;

import java.util.List;

import ca.vanzyl.provisio.model.File;
import com.google.common.collect.Lists;

public class Delete {

  private List<File> files = Lists.newArrayList();

  public List<File> getFiles() {
    return files;
  }
}
