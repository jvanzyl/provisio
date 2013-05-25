package io.provis.provision;

import io.provis.model.ProvisioModel;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class ProvisioningRequest {
  
  private File outputDirectory;
  private String localRepository;
  private ProvisioModel runtimeAssembly;
  private Map<String, String> versionMap;
  
  public File getOutputDirectory() {
    return outputDirectory;
  }

  public ProvisioningRequest setOutputDirectory(File outputDirectory) {
    if (outputDirectory == null) {
      this.outputDirectory = new File("").getAbsoluteFile();
    } else {
      this.outputDirectory = outputDirectory.getAbsoluteFile();
    }
    return this;
  }

  public String getLocalRepository() {
    return localRepository;
  }

  public void setLocalRepository(String localRepository) {
    this.localRepository = localRepository;
  }

  public ProvisioModel getRuntimeAssembly() {
    return runtimeAssembly;
  }

  public ProvisioningRequest setRuntimeAssembly(ProvisioModel runtimeAssembly) {
    this.runtimeAssembly = runtimeAssembly;
    return this;
  }
  
  //
  // VersionMap
  //
  public Map<String, String> getVersionMap() {
    return versionMap;
  }

  public ProvisioningRequest setVersionMap(Map<String, String> versionMap) {
    this.versionMap = versionMap;
    return this;
  }
  
  public void addVersionMap(Map<String, String> versionMap) {
    this.versionMap = versionMap;
  }  
}
