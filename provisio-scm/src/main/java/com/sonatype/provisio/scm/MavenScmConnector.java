package com.sonatype.provisio.scm;

import io.tesla.proviso.spi.ProvisioningException;

import java.io.File;

import javax.inject.Named;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;


@Named("maven-scm")
public class MavenScmConnector implements ScmConnector {

  public ScmRepository checkout(String uri, File directory) {
    uri = parseUri(uri);

    DefaultPlexusContainer container;
    try {
      container = new DefaultPlexusContainer();
    } catch (PlexusContainerException e) {
      throw new ProvisioningException(e);
    }

    try {
      ScmManager scmManager = container.lookup(ScmManager.class);
      org.apache.maven.scm.repository.ScmRepository scmRepo = scmManager.makeScmRepository(uri);
      ScmProvider scmProvider = scmManager.getProviderByUrl(uri);
      scmManager.checkOut(scmRepo, new ScmFileSet(directory));

      return new MavenScmRepository(scmProvider, scmRepo, directory);
    } catch (ComponentLookupException e) {
      throw new UnsupportedScmException(e);
    } catch (NoSuchScmProviderException e) {
      throw new UnsupportedScmException(e);
    } catch (ScmRepositoryException e) {
      throw new ProvisioningException(e);
    } catch (ScmException e) {
      throw new ProvisioningException(e);
    } finally {
      container.setLookupRealm(null);
      container.dispose();
    }
  }

  private String parseUri(String uri) {
    if (!uri.startsWith("scm:")) {
      String type = ScmUriUtils.guessScmType(uri);
      if (type == null || type.length() <= 0) {
        throw new UnsupportedScmException(uri);
      }
      uri = "scm:" + type + ":" + uri;
    }
    return uri;
  }

}
