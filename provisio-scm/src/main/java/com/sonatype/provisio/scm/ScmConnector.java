package com.sonatype.provisio.scm;

import java.io.File;

public interface ScmConnector {

  ScmRepository checkout(String uri, File directory);

}
