package io.provis.jenkins.config.jgit;

import java.io.IOException;

import io.provis.jenkins.config.ConfigurationMixin;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;
import io.provis.jenkins.config.templates.TemplateList;

public class JGitToolConfig implements ConfigurationMixin {
  
  public JGitToolConfig() {
  }
  
  @Override
  public String getId() {
    return "jgit";
  }

  @Override
  public void configure(MasterConfigurationBuilder builder) throws IOException {
    builder.templates(TemplateList.list(JGitToolConfig.class));
  }

}
