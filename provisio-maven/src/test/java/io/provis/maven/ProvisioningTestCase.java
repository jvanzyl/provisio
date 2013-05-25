package io.provis.maven;

import io.tesla.aether.guice.maven.MavenBehaviourModule;

import org.eclipse.sisu.containers.InjectedTestCase;

import com.google.inject.Binder;

public abstract class ProvisioningTestCase extends InjectedTestCase {

  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }
}
