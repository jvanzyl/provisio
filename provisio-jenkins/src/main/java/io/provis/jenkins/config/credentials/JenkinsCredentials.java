package io.provis.jenkins.config.credentials;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JenkinsCredentials {

  private Map<String, Domain> domains = new HashMap<>();

  public Collection<Domain> getDomains() {
    return domains.values();
  }

  public JenkinsCredentials userCredential(String id, String username, String password) {
    return userCredential(id, username, password, null);
  }

  public JenkinsCredentials userCredential(String id, String username, String password, String domain) {
    return userCredential(id, id, username, password, domain);
  }

  public JenkinsCredentials userCredential(String id, String description, String username, String password, String domain) {
    return userCredential(id, description, username, password, domain, domain);
  }

  public JenkinsCredentials userCredential(String id, String description, String username, String password, String domain, String domainDescription) {
    domain(domain, domainDescription).getUsernamePasswordCredentials().add(new UsernamePassword(id, description, username, password));
    return this;
  }

  public JenkinsCredentials secretCredential(String id, String secret) {
    return secretCredential(id, secret, null);
  }

  public JenkinsCredentials secretCredential(String id, String secret, String domain) {
    return secretCredential(id, id, secret, domain);
  }

  public JenkinsCredentials secretCredential(String id, String description, String secret, String domain) {
    return secretCredential(id, description, secret, domain, domain);
  }

  public JenkinsCredentials secretCredential(String id, String description, String secret, String domain, String domainDescription) {
    domain(domain, domainDescription).getSecretCredentials().add(new SecretCredential(id, description, secret));
    return this;
  }

  public void merge(JenkinsCredentials creds) {
    if(creds != null && creds.domains != null) {
      Set<String> keys = new HashSet<>(creds.domains.keySet());
      for(String key: keys) {
        Domain thatDomain = creds.domains.get(key);
        Domain thisDomain = domains.get(key);
        if(thisDomain == null) {
          domains.put(key, thatDomain);
        } else {
          merge(thatDomain, thisDomain);
        }
      }
    }
  }

  private static void merge(Domain from, Domain to) {
    to.getSecretCredentials().addAll(from.getSecretCredentials());
    to.getUsernamePasswordCredentials().addAll(from.getUsernamePasswordCredentials());
  }

  private Domain domain(String url, String description) {
    Domain domain = domains.get(url);
    if (domain == null) {
      if (url == null) {
        domain = new Domain(url, description, null, null);
      } else {
        URI serverUri = URI.create(url);
        domain = new Domain(url, description, serverUri.getScheme(), serverUri.getHost());
      }
      domains.put(url, domain);
    }
    return domain;
  }

}
