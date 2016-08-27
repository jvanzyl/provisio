package io.provis.jenkins.config.security.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.ConfigurationMixin;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;
import io.provis.jenkins.config.templates.TemplateList;

public class UserConfig implements ConfigurationMixin {

  private List<User> users = new ArrayList<>();

  @Override
  public UserConfig init(Configuration config) {
    config
      .partition()
      .forEach((k, c) -> {
        User user = new User(k);
        c.value("email", user::email)
          .value("apiToken", user::apiToken);
        user(user);
      });
    return this;
  }

  public UserConfig user(User user) {
    users.add(user);
    return this;
  }

  public List<User> getUsers() {
    return users;
  }

  @Override
  public String getId() {
    return "users";
  }

  @Override
  public void configure(MasterConfigurationBuilder builder) throws IOException {
    builder.templates(TemplateList.of(UserConfig.class)
      .multiply(users, "user", (n, u) -> "users/" + u.getName() + "/" + n));
  }

  public static class User {
    private String name;
    private String email;
    private String apiToken;

    public User(String name) {
      this.name = name;
    }

    public User email(String email) {
      this.email = email;
      return this;
    }

    public User apiToken(String apiToken) {
      this.apiToken = apiToken;
      return this;
    }

    public String getName() {
      return name;
    }

    public String getEmail() {
      return email;
    }

    public String getApiToken() {
      return apiToken;
    }
  }

}
