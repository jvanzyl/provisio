package io.provis.provision.action.artifact;

import io.provis.model.ProvisioArtifact;
import io.provis.parser.ProvisioModelParser;
import io.provis.provision.model.ProvisoArtifactMetadata;

import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Look for actions defined in metadata within artifacts.
 * 
 * @author jvanzyl
 * 
 */
@Named
@Singleton
public class ArtifactMetadataGleaner {

  private ProvisioModelParser reader;

  @Inject
  private ArtifactMetadataGleaner(ProvisioModelParser reader) {
    this.reader = reader;
  }

  public ProvisoArtifactMetadata gleanMetadata(ProvisioArtifact artifact) {
    try {
      JarFile jarFile = new JarFile(artifact.getFile());
      ZipEntry metadata = jarFile.getEntry("metadata.proviso");
      if (metadata != null) {
        InputStream is = jarFile.getInputStream(metadata);
        if (is != null) {
          //return reader.readArtifactMetadata(is);
        }
      }
      return null;
    } catch (Exception e) {
      //
      // Handle error in reading JAR files. If there are malformed entries just ignore them.
      //
      return null;
    }
  }
}
