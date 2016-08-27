package io.provis.jenkins.config;

import java.io.IOException;

import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;

public interface ConfigurationMixin {
  
  String getId();
  
  default void configure(MasterConfigurationBuilder builder) throws IOException {
  }
  
  default ConfigurationMixin init(Configuration config) {
    return this;
  }
}
