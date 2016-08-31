package io.provis.jenkins.config;

import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;

public interface ConfigurationMixin {
  
  String getId();
  
  default void configure(MasterConfigurationBuilder builder) {
  }
  
  default ConfigurationMixin init(Configuration config) {
    return this;
  }
}
