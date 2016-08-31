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
import io.provis.jenkins.config.crypto.SecretEncryptorFactory;
import io.provis.jenkins.config.templates.TemplateList;
import io.provis.jenkins.config.templates.TemplateSource;

public class MasterConfiguration {

  private String url;
  private int port;

  private final Configuration configuration;
  private final SecretEncryptorFactory encFactory;
  private final List<TemplateList> templates;
  private final List<ConfigurationMixin> mixins;

  public MasterConfiguration(
    String url,
    int port,
    Configuration configuration,
    SecretEncryptorFactory encFactory,
    List<TemplateList> templates,
    List<ConfigurationMixin> mixins) {

    this.url = url;
    this.port = port;
    this.configuration = configuration;
    this.encFactory = encFactory;
    this.templates = templates;
    this.mixins = mixins;
  }

  public String getUrl() {
    return url;
  }

  public int getPort() {
    return port;
  }

  public <T extends ConfigurationMixin> T getConfig(Class<T> cl) {
    return getConfig(mixins, cl);
  }

  public void write(File outputDirectory) throws IOException {
    outputDirectory.mkdirs();

    // create template context
    Map<String, Object> context = new HashMap<>();
    if (configuration != null) {
      for (Map.Entry<String, String> e : configuration.entrySet()) {
        context.put(e.getKey(), e.getValue());
      }
    }
    for (ConfigurationMixin mixin : mixins) {
      context.put(mixin.getId(), mixin);
    }

    // add secret encryption
    context.put("encryptSecret", (Function<String, String>) t -> encFactory.encryptor("hudson.util.Secret").encrypt(t));

    Object[] contexts = new Object[] {this, context};

    // process templates
    TemplateProcessor processor = new TemplateProcessor();
    for (TemplateSource ts : TemplateList.combined(templates).getTemplates()) {
      processor.fromTemplate(ts, contexts, outputDirectory);
    }

    // write encryption keys
    File secrets = new File(outputDirectory, "secrets");
    secrets.mkdirs();
    encFactory.write(secrets);
  }

  public static MasterConfiguration fromConfig(Properties props) throws IOException {
    return builder().properties(props).build();
  }

  public static MasterConfigurationBuilder builder() {
    return new MasterConfigurationBuilder();
  }

  public static MasterConfigurationBuilder builder(ClassLoader loader) {
    return new MasterConfigurationBuilder(loader);
  }

  public static class MasterConfigurationBuilder {

    private final ClassLoader classLoader;

    private String url;
    private int port = -1;
    private Configuration configuration;
    private SecretEncryptorFactory encFactory = new SecretEncryptorFactory();

    private List<TemplateList> templates = new ArrayList<>();
    private List<ConfigurationMixin> mixins = new ArrayList<>();

    public MasterConfigurationBuilder() {
      this(null);
    }

    public MasterConfigurationBuilder(ClassLoader loader) {
      if (loader == null) {
        loader = Thread.currentThread().getContextClassLoader();
      }
      this.classLoader = loader;
      templates(TemplateList.list(MasterConfiguration.class, "base"));
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public MasterConfigurationBuilder jenkins(String url, int port) {
      this.url = url;
      this.port = port;
      return this;
    }

    public MasterConfigurationBuilder configuration(File propertiesFile) {
      return configuration(new Configuration(propertiesFile));
    }

    public MasterConfigurationBuilder properties(Properties properties) {
      Properties props = new Properties();
      props.putAll(properties);
      return configuration(new Configuration(props));
    }

    public MasterConfigurationBuilder configuration(Configuration configuration) {
      if (this.configuration != null) {
        throw new IllegalStateException("This builder is already configured");
      }

      this.configuration = configuration;
      if (url == null && port == -1) {
        jenkins(configuration.get("jenkins.url"), configuration.getInt("jenkins.port"));
      }

      String confMixins = configuration.get("config.mixins");
      if (confMixins != null) {
        addMixinsFromServices(confMixins);
      }
      return this;
    }

    public MasterConfigurationBuilder masterKey(String masterKey) {
      this.encFactory = new SecretEncryptorFactory(masterKey);
      return this;
    }

    public JenkinsCredentials credentials() {
      JenkinsCredentials credentials = getConfig(mixins, JenkinsCredentials.class);
      if (credentials == null) {
        credentials = new JenkinsCredentials();
        config(credentials);
      }
      return credentials;
    }

    public SecretEncryptorFactory encryption() {
      return encFactory;
    }

    public MasterConfigurationBuilder credentials(JenkinsCredentials credentials) {
      credentials().merge(credentials);
      return this;
    }

    public MasterConfigurationBuilder templates(TemplateList templateList) {
      if (templateList != null) {
        templates.add(templateList);
      }
      return this;
    }

    public MasterConfigurationBuilder config(ConfigurationMixin mixin) {
      mixin.configure(this);
      mixins.add(mixin);
      return this;
    }

    public MasterConfiguration build() {
      return new MasterConfiguration(
        url,
        port,
        configuration,
        encFactory,
        templates,
        mixins);
    }

    private void addMixinsFromServices(String confMixins) {
      Map<String, ConfigurationMixin> map = new HashMap<>();
      for (ConfigurationMixin m : ServiceLoader.load(ConfigurationMixin.class, getClassLoader())) {
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

  private static <T extends ConfigurationMixin> T getConfig(List<ConfigurationMixin> configs, Class<T> cl) {
    for (ConfigurationMixin c : configs) {
      if (cl.isInstance(c)) {
        return cl.cast(c);
      }
    }
    return null;
  }
}
