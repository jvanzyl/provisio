package com.sonatype.provisio.scm;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

@Named
public class ScmManager {

  @Inject
  private Logger log;

  @Inject
  private List<ScmConnector> connectors;

  public ScmRepository checkout(String uri, File directory) {
    directory = directory.getAbsoluteFile();

    for (ScmConnector connector : connectors) {
      log.debug("Trying connector {} for {}", connector, uri);
      try {
        return connector.checkout(uri, directory);
      } catch (UnsupportedScmException e) {
        log.debug("Connector {} failed for {}", new Object[] { connector, uri, e });
      }
    }
    throw new UnsupportedScmException("No connector among " + connectors + " supports " + uri);
  }

}
