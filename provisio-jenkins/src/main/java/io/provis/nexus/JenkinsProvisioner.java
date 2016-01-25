/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.nexus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.swizzle.stream.ReplaceVariablesInputStream;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.provis.provision.SimpleProvisioner;

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
    // http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/1.644/jenkins-war-1.644.war
    File jenkinsWar = resolveFromServer(
      String.format("%s/org/jenkins-ci/main/jenkins-war/%s/jenkins-war-%s.war", JENKINS_CENTRAL, version, version),
      "org.jenkins-ci.main:jenkins-war:war:" + context.getVersion());

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
        Map<String,String> variables = Maps.newHashMap();
        variables.put("PORT", context.getPort()+"");
        InputStream replace = new ReplaceVariablesInputStream(is, "@", "@", variables);
        ByteStreams.copy(replace, os);
        jenkinsScript.setExecutable(true);
      }
    }
    // Add plugins
    for (String plugin : context.getPlugins()) {
      addPlugin(plugin, workDirectory);
    }
    return installationDirectory;
  }

  // Add a plugin to the Jenkins installation
  public void addPlugin(String coord, File workDirectory) throws IOException {
    File pluginDirectory = new File(workDirectory, "plugins");
    File pluginFile = resolveFromRepository(JENKINS_CENTRAL, coord);
    FileUtils.mkdir(pluginDirectory.getAbsolutePath());
    // git-client-1.9.2.hpi
    String pluginNameWithoutVersion = pluginFile.getName().substring(0, pluginFile.getName().lastIndexOf("-")) + ".hpi";
    File pluginFileWithoutVersion = new File(pluginDirectory, pluginNameWithoutVersion);
    Files.copy(pluginFile, pluginFileWithoutVersion);
  }
}
