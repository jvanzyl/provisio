package io.provis.jenkins.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.XmlFile;
import hudson.util.CopyOnWriteMap;
import hudson.util.Secret;
import hudson.util.TextFile;
import jenkins.model.Jenkins;
import jenkins.util.io.FileBoolean;

public class EmbeddedJenkinsRuntime implements JenkinsRuntime {

  private static final String CREDENTIALS = "credentials.xml";
  
  private File rootDir;

  public EmbeddedJenkinsRuntime(File rootDir, byte[] secretKey) {
    this.rootDir = rootDir;
    try {
      init(secretKey);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to initialize jenkins runtime", e);
    }
  }
  
  public void init(byte[] secretKey) throws IOException {
    Jenkins jenkins = Jenkins.setup(rootDir, secretKey);
    
    TextFile secretFile = new TextFile(new File(rootDir, "secret.key"));
    secretFile.write(jenkins.getSecretKey());
    
    // don't let jenkins complain about old encryption secret scheme
    new FileBoolean(new File(rootDir, "secret.key.not-so-secret")).on();
    // but force rekey on start
    new FileBoolean(new File(new File(rootDir, "jenkins.security.RekeySecretAdminMonitor"), "scanOnBoot")).on();
  }
  
  @Override
  public void close() {
    Jenkins.teardown();
  }
  
  @Override
  public String encrypt(String value) {
    return Secret.fromString(value).getEncryptedValue();
  }
  
  public void writeCredentials(CredentialContainer creds) throws IOException {
    
    Map<Domain, List<Credentials>> domainCredentialsMap = new CopyOnWriteMap.Hash<Domain, List<Credentials>>();
    List<Credentials> globalCredentials = new CopyOnWriteArrayList<Credentials>();
    domainCredentialsMap.put(Domain.global(), globalCredentials);
    
    for (UsernamePassword up : creds.getUsernamePasswordCredentials()) {
      addUsernamePasswordCredential(globalCredentials, up);
    }
    for (SecretCredential secret : creds.getSecretCredentials()) {
      addSecretCredential(globalCredentials, secret);
    }
    for (GitHubCredential gitHubCredential : creds.getGitHubCredentials()) {
      addGitHubCredentialFromToken(domainCredentialsMap, gitHubCredential);
    }
    SystemCredentialsProvider systemCredentialProvider = new SystemCredentialsProvider();
    File credentialsFile = new File(rootDir, CREDENTIALS);
    XmlFile xml = new XmlFile(credentialsFile);
    
    // Add the field with reflection to avoid triggering more Jenkins internal machinery
    try {
      Field domainCredentialsMapField = systemCredentialProvider.getClass().getDeclaredField("domainCredentialsMap"); //NoSuchFieldException
      domainCredentialsMapField.setAccessible(true);
      domainCredentialsMapField.set(systemCredentialProvider, domainCredentialsMap); //IllegalAccessException
    } catch(IllegalAccessException | NoSuchFieldException e) {
      throw new IOException("Error accessing field " + systemCredentialProvider.getClass() + ".domainCredentialsMap", e);
    }
    xml.write(systemCredentialProvider);
  }

  private void addUsernamePasswordCredential(List<Credentials> globalCredentials, UsernamePassword secret) throws IOException {
    globalCredentials.add(new UsernamePasswordCredentialsImpl(
      CredentialsScope.GLOBAL,
      id(secret.getId()),
      secret.getId(),
      secret.getUsername(),
      secret.getPassword()));
  }

  private void addSecretCredential(List<Credentials> globalCredentials, SecretCredential secret) throws IOException {
    globalCredentials.add(new StringCredentialsImpl(
      CredentialsScope.GLOBAL,
      id(secret.getId()),
      secret.getId(),
      Secret.fromString(secret.getSecret())));
  }

  private void addGitHubCredentialFromToken(Map<Domain, List<Credentials>> domainCredentialsMap, GitHubCredential gitHubCredential) {
    List<Credentials> gitHubCredentialsForDomain = new CopyOnWriteArrayList<Credentials>();
    URI serverUri = URI.create(gitHubCredential.getGitHubApiUrl());
    List<DomainSpecification> specifications = Arrays.asList(
      new SchemeSpecification(serverUri.getScheme()),
      new HostnameSpecification(serverUri.getHost(), null));
    
    Domain domain = new Domain(serverUri.getHost(), "GitHub domain (autogenerated)", specifications);
    String description = String.format("GitHub (%s) auto generated token credentials for %s", gitHubCredential.getGitHubApiUrl(), gitHubCredential.getUsername());
    StringCredentialsImpl creds = new StringCredentialsImpl(
      CredentialsScope.GLOBAL,
      id(gitHubCredential.getId()),
      description,
      Secret.fromString(gitHubCredential.getOauthToken()));
    gitHubCredentialsForDomain.add(creds);
    domainCredentialsMap.put(domain, gitHubCredentialsForDomain);
  }
  
  private static String id(String id) {
    return id != null ? id : UUID.randomUUID().toString();
  }
}
