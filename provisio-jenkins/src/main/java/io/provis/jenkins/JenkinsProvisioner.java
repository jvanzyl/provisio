/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.jenkins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.swizzle.stream.ReplaceVariablesInputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.provis.SimpleProvisioner;
import io.tesla.proviso.archive.UnArchiver;

@Named(JenkinsProvisioner.ID)
public class JenkinsProvisioner extends SimpleProvisioner {

  public static final String ID = "jenkins";
  private static final String JENKINS_CENTRAL = "http://repo.jenkins-ci.org/public";

  public File provision(JenkinsProvisioningContext context) throws IOException {
    String version = context.getVersion();
    File installationDirectory = context.getInstallationDirectory();
    File workDirectory = context.getWorkDirectory();
    if (version.length() <= 0) {
      throw new IllegalArgumentException("Jenkins version not specified");
    }
    String repositoryUrl = context.getRepositoryUrl();
    if(repositoryUrl == null) {
      repositoryUrl = JENKINS_CENTRAL;
    }
    // http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/1.644/jenkins-war-1.644.war
    File jenkinsWar = resolveFromRepository(repositoryUrl, "org.jenkins-ci.main:jenkins-war:war:" + context.getVersion());

    // Create the installation and work directories
    FileUtils.mkdir(installationDirectory.getAbsolutePath());
    FileUtils.mkdir(workDirectory.getAbsolutePath());
    // Copy Jenkins WAR into the installation directory
    Files.copy(jenkinsWar, new File(installationDirectory, jenkinsWar.getName()));
    // Create a startup script based on the provisioning context to help with debugging
    File jenkinsScript = new File(installationDirectory, "jenkins.sh");
    try (InputStream is = JenkinsProvisioner.class.getClassLoader().getResourceAsStream("scripts/jenkins.sh");
      OutputStream os = new FileOutputStream(jenkinsScript)) {
      if (is != null) {
        Map<String, String> variables = Maps.newHashMap();
        variables.put("PORT", context.getPort() + "");
        InputStream replace = new ReplaceVariablesInputStream(is, "@", "@", variables);
        ByteStreams.copy(replace, os);
        jenkinsScript.setExecutable(true);
      }
    }
    // 
    // Find the plugins that are bundled with this version of Jenkins
    //
    Set<String> bundledPluginIds = findBundledPluginIds(jenkinsWar);
    //
    // Adding plugins accounting for default plugins packaged with the Jenkins WAR file 
    // that we want to replace. We need to expand the plugin and place a .timestamp2 file
    //
    UnArchiver unarchiver = UnArchiver.builder().build();
    for (String plugin : context.getPlugins()) {
      File pluginsDirectory = new File(workDirectory, "plugins");
      File pluginFile = resolveFromRepository(JENKINS_CENTRAL, plugin);
      FileUtils.mkdir(pluginsDirectory.getAbsolutePath());
      // git-client
      String pluginId = pluginFile.getName().substring(0, pluginFile.getName().lastIndexOf("-"));
      // git-client.hpi
      String pluginFileNameWithoutVersion = pluginId + ".jpi";
      File pluginFileWithoutVersion = new File(pluginsDirectory, pluginFileNameWithoutVersion);
      Files.copy(pluginFile, pluginFileWithoutVersion);
      //
      // Pinned plugin: https://wiki.jenkins-ci.org/display/JENKINS/Pinned+Plugins
      //
      // This prevents versions that we have provisioned from being overwritten by a version that is present
      // in the Jenkins WAR while Jenkins is starting up. I ran into this problem first when the latest version
      // of Jenkins available came with a credentials.jpi that wasn't new enough for a particular plugin to
      // use. So I provisioned what I needed and unless I marked it as pinned it was overwritten by the startup
      // process screwing up my tests.
      //
      if (bundledPluginIds.contains(pluginId)) {
        String pinnedName = pluginId + ".jpi.pinned";
        File pinned = new File(pluginsDirectory, pinnedName);
        Files.touch(pinned);
      }
      // Expand
      File pluginDirectory = new File(pluginsDirectory, pluginId);
      unarchiver.unarchive(pluginFileWithoutVersion, pluginDirectory);
    }
    return installationDirectory;
  }

  public Set<String> findBundledPluginIds(File jenkinsWar) throws IOException {
    Set<String> bundledPluginIds = Sets.newHashSet();
    try (ZipFile file = new ZipFile(jenkinsWar)) {
      Enumeration<? extends ZipEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();
        if (name.startsWith("WEB-INF/plugins/") && name.endsWith(".hpi")) {
          // WEB-INF/plugins/credentials.hpi --> credentials
          name = name.substring(name.lastIndexOf("/") + 1);
          name = name.substring(0, name.lastIndexOf("."));
          bundledPluginIds.add(name);
        }
      }
    }
    return bundledPluginIds;
  }
}
