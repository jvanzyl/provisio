package io.provis.jenkins.test;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.ConfigurationMixin;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;
import io.provis.jenkins.config.templates.TemplateList;

public class TestConfig implements ConfigurationMixin {

  private String foo;
  
  public String getFoo() {
    return foo;
  }
  
  public ConfigurationMixin init(Configuration config) {
    config.value("foo", v -> this.foo = v);
    return this;
  }
  
  public String getId() {
    return "test";
  }
  
  public void configure(MasterConfigurationBuilder builder) {
    builder.templates(TemplateList.list(TestConfig.class));
  }

}
