package io.provis.jenkins.config.security.ad;

import java.io.IOException;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.ConfigurationMixin;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;
import io.provis.jenkins.config.templates.TemplateList;

public class ActiveDirectoryConfig implements ConfigurationMixin {

  private String domainName;
  private String bindDN;
  private String bindPassword;
  private String site;
  private String server;
  private GroupLookupStrategy groupLookupStrategy = GroupLookupStrategy.AUTO;
  private Cache cache = new Cache(Cache.SIZE_256, Cache.TTL_10M);

  public ActiveDirectoryConfig() {}

  public ActiveDirectoryConfig(String domainName) {
    this.domainName = domainName;
  }

  @Override
  public ActiveDirectoryConfig init(Configuration config) {
    domainName = config.get("domain");
    return server(config.get("server"))
      .site(config.get("site"))
      .bind(
        config.get("bindDN"),
        config.get("bindPassword"))
      .groupLookupStrategy(GroupLookupStrategy.forName(config.get("lookup")))
      .cache(
        config.getInt("cache.size", Cache.SIZE_256),
        config.getInt("cache.ttl", Cache.TTL_10M));
  }

  public ActiveDirectoryConfig server(String server) {
    if (server != null) {
      String[] servers = server.split(",");
      for (int i = 0; i < servers.length; i++) {
        if (!servers[i].contains(":")) {
          servers[i] += ":3268";
        }
      }
      server = String.join(",", servers);
    }
    this.server = server;
    return this;
  }

  public ActiveDirectoryConfig site(String site) {
    this.site = site;
    return this;
  }

  public ActiveDirectoryConfig bind(String dn, String password) {
    this.bindDN = dn;
    this.bindPassword = password;
    return this;
  }

  public ActiveDirectoryConfig groupLookupStrategy(GroupLookupStrategy groupLookupStrategy) {
    this.groupLookupStrategy = groupLookupStrategy;
    return this;
  }

  public ActiveDirectoryConfig noCache() {
    this.cache = null;
    return this;
  }

  public ActiveDirectoryConfig cache(int size, int ttl) {
    this.cache = new Cache(size, ttl);
    return this;
  }

  public boolean isCustom() {
    return domainName != null;
  }

  public String getDomainName() {
    return domainName;
  }

  public String getSite() {
    return site;
  }

  public String getServer() {
    return server;
  }

  public String getBindDN() {
    return bindDN;
  }

  public String getBindPassword() {
    return bindPassword;
  }

  public GroupLookupStrategy getGroupLookupStrategy() {
    return groupLookupStrategy;
  }

  public Cache getCache() {
    return cache;
  }

  @Override
  public String getId() {
    return "security.ad";
  }

  @Override
  public void configure(MasterConfigurationBuilder builder) throws IOException {
    builder.templates(TemplateList.list(ActiveDirectoryConfig.class));
  }

  public enum GroupLookupStrategy {
    AUTO, RECURSIVE, CHAIN;

    static GroupLookupStrategy forName(String name) {
      for (GroupLookupStrategy s : values()) {
        if (s.name().equalsIgnoreCase(name)) {
          return s;
        }
      }
      return AUTO;
    }
  }

  public static class Cache {
    // constants that correspond to AD plugin ui choices
    public static final int SIZE_10 = 10;
    public static final int SIZE_20 = 20;
    public static final int SIZE_50 = 50;
    public static final int SIZE_100 = 100;
    public static final int SIZE_200 = 200;
    public static final int SIZE_256 = 256;
    public static final int SIZE_500 = 500;
    public static final int SIZE_1000 = 1000;

    public static final int TTL_30S = 30;
    public static final int TTL_1M = 60;
    public static final int TTL_5M = 300;
    public static final int TTL_10M = 600;
    public static final int TTL_15M = 900;
    public static final int TTL_30M = 1800;
    public static final int TTL_1H = 3600;

    private int size;
    private int ttl;

    public Cache(int size, int ttl) {
      this.size = size;
      this.ttl = ttl;
    }

    public int getSize() {
      return size;
    }

    public int getTtl() {
      return ttl;
    }
  }

}
