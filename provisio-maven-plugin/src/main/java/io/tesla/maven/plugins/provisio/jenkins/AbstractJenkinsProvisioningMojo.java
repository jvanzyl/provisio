package io.tesla.maven.plugins.provisio.jenkins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.crypto.ConfigCrypto;
import io.takari.incrementalbuild.Incremental;

public abstract class AbstractJenkinsProvisioningMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(defaultValue = "${project}")
  @Incremental(configuration = Incremental.Configuration.ignore)
  protected MavenProject project;

  @Parameter(required = true, defaultValue = "${basedir}/src/main/provisio")
  protected File descriptorDirectory;

  @Parameter(required = false, property = "encryptionKey")
  protected String encryptionKey;

  protected List<File> descriptors() {
    List<File> descriptors = new ArrayList<>();
    for (File f : descriptorDirectory.listFiles()) {
      if (f.isFile() && f.getName().endsWith(".properties")) {
        descriptors.add(f);
      }
    }
    return descriptors;
  }

  protected Configuration getConfig(File desc) {
    Configuration conf = new Configuration(desc);
    conf.putAll(project.getProperties());
    conf.set("project.groupId", project.getGroupId());
    conf.set("project.artifactId", project.getArtifactId());
    conf.set("project.version", project.getVersion());

    conf.decryptValues(encryptionKey);
    return conf;
  }

  protected ConfigCrypto crypto() {
    return new ConfigCrypto(encryptionKey);
  }

}
