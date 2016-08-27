package io.provis.jenkins.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

import io.provis.jenkins.config.credentials.JenkinsCredentials;
import io.provis.jenkins.config.crypto.SecretEncryptor;
import io.provis.jenkins.config.crypto.SecretEncryptorFactory;
import io.provis.jenkins.config.templates.TemplateList;
import io.provis.jenkins.config.templates.TemplateSource;

// Credentials
// Users
// Security
public class MasterConfiguration {

  private String url;
  private int port;

  private final Configuration configuration;
  private final String masterKey;
  private final JenkinsCredentials credentials;
  private final List<TemplateList> templates;
  private final List<ConfigurationMixin> mixins;

  public MasterConfiguration(
    String url,
    int port,
    Configuration configuration,
    String masterKey,
    JenkinsCredentials credentials,
    List<TemplateList> templates,
    List<ConfigurationMixin> mixins) {

    this.url = url;
    this.port = port;
    this.configuration = configuration;
    this.masterKey = masterKey;
    this.credentials = credentials;
    this.templates = templates;
    this.mixins = mixins;
  }

  public String getUrl() {
    return url;
  }

  public int getPort() {
    return port;
  }

  public JenkinsCredentials getCredentials() {
    return credentials;
  }
  
  public <T extends ConfigurationMixin> T getConfig(Class<T> cl) {
    for(ConfigurationMixin c: mixins) {
      if(cl.isInstance(c)) {
        return cl.cast(c);
      }
    }
    return null;
  }

  public void write(File outputDirectory) throws IOException {
    outputDirectory.mkdirs();

    SecretEncryptorFactory sef = new SecretEncryptorFactory(new File(outputDirectory, "secrets"), masterKey);
    SecretEncryptor secret = sef.newEncryptor("hudson.util.Secret", true);
    Map<String, Object> context = new HashMap<>();
    if (configuration != null) {
      for (Map.Entry<String, String> e : configuration.entrySet()) {
        context.put(e.getKey(), e.getValue());
      }
    }
    context.put("encryptSecret", (Function<String, String>) t -> secret.encrypt(t));

    for (ConfigurationMixin mixin : mixins) {
      context.put(mixin.getId(), mixin);
    }

    Object[] contexts = new Object[] {this, context};

    // combine templates
    TemplateProcessor processor = new TemplateProcessor();
    List<TemplateList> templates = new ArrayList<>();
    templates.add(TemplateList.list(MasterConfiguration.class, "base"));
    templates.addAll(this.templates);

    for (TemplateSource ts : TemplateList.combined(templates).getTemplates()) {
      processor.fromTemplate(ts, contexts, outputDirectory);
    }
  }

  public static MasterConfigurationBuilder builder() {
    return new MasterConfigurationBuilder();
  }
  
  public static MasterConfigurationBuilder builder(Properties props) {
    return builder().properties(props);
  }

  public static class MasterConfigurationBuilder {

    String url;
    int port = -1;
    Configuration configuration;
    String masterKey;
    JenkinsCredentials credentials = new JenkinsCredentials();
    List<TemplateList> templates = new ArrayList<>();
    List<ConfigurationMixin> mixins = new ArrayList<>();

    boolean servicesLoaded = false;
    
    public MasterConfigurationBuilder jenkins(String url, int port) {
      this.url = url;
      this.port = port;
      return this;
    }

    public MasterConfigurationBuilder configuration(File propertiesFile) throws IOException {
      return configuration(new Configuration(propertiesFile));
    }

    public MasterConfigurationBuilder properties(Properties properties) {
      Properties props = new Properties();
      props.putAll(properties);
      return configuration(new Configuration(props));
    }
    
    public MasterConfigurationBuilder configuration(Configuration configuration) {
      this.configuration = configuration;
      if(url == null) {
        url = configuration.get("jenkins.url");
      }
      if(port == -1) {
        port = configuration.getInt("jenkins.port");
      }
      return this;
    }

    public MasterConfigurationBuilder masterKey(String masterKey) {
      this.masterKey = masterKey;
      return this;
    }

    public JenkinsCredentials credentials() {
      return credentials;
    }

    public MasterConfigurationBuilder credentials(JenkinsCredentials credentials) {
      this.credentials.merge(credentials);
      return this;
    }

    public MasterConfigurationBuilder templates(TemplateList templateList) {
      templates.add(templateList);
      return this;
    }

    public MasterConfigurationBuilder config(ConfigurationMixin mixin) throws IOException {
      mixin.configure(this);
      mixins.add(mixin);
      return this;
    }
    
    /**
     * If not called manually, mixins defined in 'config.mixins' property will be initialized during {@linkplain #build()}.
     * If some configuration (like template overrides) needs to be added after such mixins are initialized, one can force it this method.
     */
    public MasterConfigurationBuilder forceServiceConfig() throws IOException {
      if (!servicesLoaded) {
        servicesLoaded = true;
        if (configuration != null) {
          String confMixins = configuration.get("config.mixins");
          if (confMixins != null) {
            addMixinsFromServices(confMixins);
          }
        }
      }
      return this;
    }

    public MasterConfiguration build() throws IOException {
      forceServiceConfig();
      return new MasterConfiguration(
        url,
        port,
        configuration,
        masterKey,
        credentials,
        templates,
        mixins);
    }

    private void addMixinsFromServices(String confMixins) throws IOException {

      Map<String, ConfigurationMixin> map = new HashMap<>();
      for (ConfigurationMixin m : ServiceLoader.load(ConfigurationMixin.class)) {
        map.put(m.getId(), m);
      }
      for (String id : confMixins.split(",")) {
        id = id.trim();
        ConfigurationMixin m = map.get(id);
        if (m == null) {
          throw new IllegalArgumentException("Mixin " + id + " does not exist, existing: " + map.keySet());
        }

        config(m.init(configuration.subset(m.getId())));
      }
    }
  }
}
