package io.provis.jenkins.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.inject.Named;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import io.provis.SimpleProvisioner;
import io.provis.jenkins.runtime.JenkinsRuntime;
import io.provis.jenkins.runtime.JenkinsRuntimeSPI;

@Named(JenkinsConfigRuntimeProvisioner.ID)
public class JenkinsConfigRuntimeProvisioner extends SimpleProvisioner {
  
  public static final String ID = "jenkins";
  
  private static final String DEFAULT_RUNTIME;
  static {
    Properties config = new Properties();
    try {
      try(InputStream in = JenkinsConfigRuntimeProvisioner.class.getResourceAsStream("/config.properties")) {
        config.load(in);
      }
    } catch(Exception e) {
      throw new IllegalStateException("Error loading config.properties", e);
    }
    DEFAULT_RUNTIME = config.getProperty("defaultRuntime");
  }
  
  public JenkinsConfigRuntimeProvisioner() {
    this(DEFAULT_LOCAL_REPO, DEFAULT_REMOTE_REPO);
  }
  
  public JenkinsConfigRuntimeProvisioner(File localRepo, String remoteRepo) {
    super(localRepo, remoteRepo);
  }
  
  public JenkinsRuntime provision(File rootDir, byte[] secretKey) throws IOException {
    return provision(rootDir, DEFAULT_RUNTIME, secretKey);
  }
  
  public JenkinsRuntime provision(File rootDir, String runtimeCoordinates, byte[] secretKey) throws IOException {
    
    /*
     * provisio-jenkins-runtime is a shaded jar that has a number of original jenkins stuff embedded.
     * In order to keep the classpath clear, we're using a separate classrealm only for that shaded jar.
     */
    
    File runtimeJar = resolveFromRepository(runtimeCoordinates);
    
    ClassWorld cw = new ClassWorld();
    ClassRealm realm;
    try {
      realm = cw.newRealm("jenkins-config-runtime", null);
    } catch (DuplicateRealmException e) {
      // impossible
      return null;
    }
    
    realm.setParentClassLoader(getClass().getClassLoader());
    realm.addURL(runtimeJar.toURI().toURL());
    
    try {
    
      JenkinsRuntimeSPI spi = ServiceLoader.load(JenkinsRuntimeSPI.class, realm).iterator().next();
      JenkinsRuntime runtime = spi.createRuntime(rootDir, secretKey);
      
      return new JenkinsRuntime(){
        
        @Override
        public void close() throws IOException {
          try {
            runtime.close();
          } finally {
            try {
              cw.disposeRealm(realm.getId());
            } catch (NoSuchRealmException e) {
            }
          }
        }

        @Override
        public void writeCredentials(CredentialContainer creds) throws IOException {
          runtime.writeCredentials(creds);
        }

        @Override
        public String encrypt(String value) {
          return runtime.encrypt(value);
        }
      };
      
    } catch(Throwable e) {
      
      try {
        cw.disposeRealm(realm.getId());
      } catch (NoSuchRealmException e1) {
      }
      
      throw e;
    }
  }
}
