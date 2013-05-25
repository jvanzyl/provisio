package com.sonatype.provisio.scm;

import java.io.Closeable;
import java.io.File;

public interface ScmRepository extends Closeable {

  File getBasedir();

  void update();

  void commit(String message);

  void close();

}
