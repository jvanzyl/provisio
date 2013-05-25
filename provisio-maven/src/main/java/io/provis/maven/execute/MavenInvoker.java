package io.provis.maven.execute;

import org.eclipse.aether.resolution.ArtifactResolutionException;

public interface MavenInvoker {

  MavenResult invoke(MavenRequest request) throws ArtifactResolutionException;

}
