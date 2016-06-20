package jenkins.model;

import java.io.File;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.servlet.ServletContext;

import hudson.LocalPluginManager;
import hudson.Lookup;
import hudson.PluginManager;
import hudson.Util;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import jenkins.security.LegacyCryptoConfidentialStore;

public class Jenkins {

  public transient final Lookup lookup = new Lookup();
  public static final PermissionGroup PERMISSIONS = Permission.HUDSON_PERMISSIONS;
  public static final Permission ADMINISTER = Permission.HUDSON_ADMINISTER;
  public transient final ServletContext servletContext;

  private String secretKey;
  private File root;
  public transient PluginManager pluginManager;

  private static final ThreadLocal<Jenkins> jenkins = new ThreadLocal<>();
  
  public Jenkins(File root, byte[] key) {
    this.root = root;
    secretKey = Util.toHexString(key);
    this.pluginManager = new LocalPluginManager(this);
    this.servletContext = null;
  }
  
  public String getSecretKey() {
    return secretKey;
  }

  public static Jenkins getInstance() {
    return jenkins.get();
  }

  public static Jenkins setup(File root) {
    byte[] random = new byte[32];
    SecureRandom sr = new SecureRandom();
    sr.nextBytes(random);
    return setup(root, random);
  }
  
  public static Jenkins setup(File root, byte[] key) {
    // force usage of legacy key for Secret encryption
    LegacyCryptoConfidentialStore.init();
    
    Jenkins j = new Jenkins(root, key);
    jenkins.set(j);
    return j;
  }
  
  public static void teardown() {
    jenkins.remove();
    
    LegacyCryptoConfidentialStore.dispose();
  }

  public File getRootDir() {
    return root;
  }

  /**
   * Gets {@linkplain #getSecretKey() the secret key} as a key for AES-128.
   * @since 1.308
   * @deprecated
   *       See {@link #getSecretKey()}.
   */
  public SecretKey getSecretKeyAsAES128() {
    return Util.toAes128Key(secretKey);
  }

  public PluginManager getPluginManager() {
    return pluginManager;
  }

}
