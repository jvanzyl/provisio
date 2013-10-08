package io.provis.maven;

import org.eclipse.sisu.launch.InjectedTestCase;

import com.google.inject.Binder;

import io.tesla.aether.guice.maven.MavenBehaviourModule;

public abstract class ProvisioningTestCase extends InjectedTestCase {

  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }
}
