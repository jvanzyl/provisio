package io.provis.ant;

import org.eclipse.aether.resolution.ArtifactResolutionException;

public interface AntInvoker {

  AntResult invoke(AntRequest request) throws ArtifactResolutionException;

}
