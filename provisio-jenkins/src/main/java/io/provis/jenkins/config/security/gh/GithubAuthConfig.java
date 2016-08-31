package io.provis.jenkins.config.security.gh;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.ConfigurationMixin;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;
import io.provis.jenkins.config.templates.TemplateList;

public class GithubAuthConfig implements ConfigurationMixin {
  
  private String webUrl = "https://github.com";
  private String apiUrl = "https://api.github.com";
  private String clientId;
  private String clientSecret;
  private String oauthScopes = "read:org,user:email";
  
  public GithubAuthConfig() {
  }
  
  @Override
  public GithubAuthConfig init(Configuration config) {
    config
      .value("webUrl", this::webUrl)
      .value("apiUrl", this::apiUrl)
      .value("clientId", this::clientId)
      .value("clientSecret", this::clientSecret)
      .value("oauthScopes", this::oauthScopes);
    return this;
  }
  
  public GithubAuthConfig webUrl(String webUrl) {
    this.webUrl = webUrl;
    return this;
  }
  
  public GithubAuthConfig apiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }
  
  public GithubAuthConfig clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }
  
  public GithubAuthConfig clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }
  
  public GithubAuthConfig oauthScopes(String oauthScopes) {
    this.oauthScopes = oauthScopes;
    return this;
  }
  
  public String getWebUrl() {
    return webUrl;
  }
  
  public String getApiUrl() {
    return apiUrl;
  }
  
  public String getClientId() {
    return clientId;
  }
  
  public String getClientSecret() {
    return clientSecret;
  }
  
  public String getOauthScopes() {
    return oauthScopes;
  }
  
  @Override
  public String getId() {
    return "security.gh";
  }

  @Override
  public void configure(MasterConfigurationBuilder builder) {
    builder.templates(TemplateList.list(GithubAuthConfig.class));
  }

}
