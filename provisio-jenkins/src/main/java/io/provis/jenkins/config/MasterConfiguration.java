package io.provis.jenkins.config;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.Lists;

import io.provis.jenkins.runtime.JenkinsRuntime;
import io.provis.jenkins.runtime.JenkinsRuntime.CredentialContainer;
import io.provis.jenkins.runtime.JenkinsRuntime.GitHubCredential;
import io.provis.jenkins.runtime.JenkinsRuntime.SecretCredential;
import io.provis.jenkins.runtime.JenkinsRuntime.UsernamePassword;

// Credentials
// Users
// Security
public class MasterConfiguration implements Closeable {
  
  private final JenkinsRuntime runtime;
  
  private final File outputDirectory;
  private final File templateDirectory;
  private final List<String> templates;
  private final Properties properties;
  
  private final List<UsernamePassword> usernamePasswordCredentials;
  private final List<SecretCredential> secretCredentials;
  private final List<GitHubCredential> gitHubCredentials;

  public MasterConfiguration(
      JenkinsRuntime runtime,
      File templateDirectory,
      List<String> templates,
      File outputDirectory,
      Properties properties,
      List<UsernamePassword> usernamePasswordCredentials,
      List<SecretCredential> secretCredentials,
      List<GitHubCredential> gitHubCredentials) {

      this.runtime = runtime;
      this.templateDirectory = templateDirectory;
      this.templates = templates;
      this.outputDirectory = outputDirectory;
      this.usernamePasswordCredentials = usernamePasswordCredentials;
      this.secretCredentials = secretCredentials;
      this.gitHubCredentials = gitHubCredentials;
      this.properties = properties;
    }

  public void write() throws IOException {
    runtime.writeCredentials(new CredentialContainer(secretCredentials, usernamePasswordCredentials, gitHubCredentials));
    writeGlobalConfiguration();
  }
  
  @Override
  public void close() throws IOException {
    runtime.close();    
  }
  
  private void writeGlobalConfiguration() throws IOException {
    outputDirectory.mkdirs();

    if(properties != null) {
      Set<Object> keys = new HashSet<>(properties.keySet());
      for(Object k: keys) {
        if(k instanceof String) {
          properties.put(k + ".encrypted", runtime.encrypt(k.toString()));
        }
      }
    }
    
    Template template = new Template(templateDirectory);
    if(templates != null) {
      for(String templateName: templates) {
        template.fromTemplate(templateName, properties, outputDirectory);
      }
    }
    for (String templateName : collectTemplates()) {
      if(templates == null && !templates.contains(templateName)) {
        template.fromTemplate(templateName, properties, outputDirectory);
      }
    }
  }

  private Collection<String> collectTemplates() {
    if(templateDirectory != null) {
      List<String> templates = new ArrayList<>();
      
      Queue<File> q = new LinkedList<>();
      q.add(templateDirectory);
      
      while(!q.isEmpty()) {
        File dir = q.remove();
        for(File f: dir.listFiles()) {
          if(f.isDirectory()) {
            q.add(f);
          } else {
            String path = f.toURI().relativize(templateDirectory.toURI()).getPath();
            templates.add(path);
          }
        }
      }
      return templates;
    }
    return Collections.emptyList();
  }
  
  // Credentials

  public static MasterConfigurationBuilder builder() {
    return new MasterConfigurationBuilder();
  }
  
  public static class MasterConfigurationBuilder {

    JenkinsConfigRuntimeProvisioner provisioner;
    File templateDirectory;
    List<String> templates = Lists.newArrayList();
    File outputDirectory;
    Properties properties;
    byte[] secretKey;
    List<UsernamePassword> usernamePasswordCredentials = Lists.newArrayList();
    List<SecretCredential> secretCredentials = Lists.newArrayList();
    List<GitHubCredential> gitHubCredentials = Lists.newArrayList();

    public MasterConfigurationBuilder outputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public MasterConfigurationBuilder properties(File propertiesFile) throws IOException {
      Properties properties = new Properties();
      if (propertiesFile != null && propertiesFile.exists()) {
        try (InputStream is = new FileInputStream(propertiesFile)) {
          properties.load(is);
        }
      }
      this.properties = properties;
      return this;
    }
    
    public MasterConfigurationBuilder provisioner(JenkinsConfigRuntimeProvisioner provisioner) {
      this.provisioner = provisioner;
      return this;
    }
    
    public MasterConfigurationBuilder secretKey(byte[] secretKey) {
      this.secretKey = secretKey;
      return this;
    }
    
    public MasterConfigurationBuilder templates(File templateDirectory) {
      this.templateDirectory = templateDirectory;
      return this;
    }
    
    public MasterConfigurationBuilder templates(String ... templates) {
      Collections.addAll(this.templates, templates);
      return this;
    }

    public MasterConfigurationBuilder templates(Collection<String> templates) {
      this.templates.addAll(templates);
      return this;
    }

    public MasterConfigurationBuilder properties(Properties properties) {
      Properties props = new Properties();
      props.putAll(properties);
      this.properties = props;
      return this;
    }

    public MasterConfigurationBuilder usernamePasswordCredential(String id, String username, String password) {
      usernamePasswordCredentials.add(new UsernamePassword(id, username, password));
      return this;
    }

    public MasterConfigurationBuilder secretCredential(String id, String secret) {
      secretCredentials.add(new SecretCredential(id, secret));
      return this;
    }

    public MasterConfigurationBuilder gitHubCredential(String id, String username, String oauthToken, String gitHubApiUrl) {
      gitHubCredentials.add(new GitHubCredential(id, username, oauthToken, gitHubApiUrl));
      return this;
    }

    public MasterConfiguration build(JenkinsRuntime runtime) throws IOException {
      return new MasterConfiguration(runtime,
          templateDirectory,
          templates,
          outputDirectory,
          properties,
          usernamePasswordCredentials,
          secretCredentials,
          gitHubCredentials);
    }
    
    public MasterConfiguration build() throws IOException {
      
      byte[] key;
      if(secretKey == null) {
        key = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(key);
      } else {
        key = secretKey;
      }
      
      JenkinsConfigRuntimeProvisioner p = provisioner;
      if(p == null) {
        p = new JenkinsConfigRuntimeProvisioner();
      }
      
      return build(p.provision(outputDirectory, key));
      
    }
  }
}
