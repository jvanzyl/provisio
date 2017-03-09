/**
 * Copyright (c) 2017 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio.jenkins;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.jenkins.config.Configuration;

@Mojo(name = "decrypt-config", requiresProject = false, requiresDependencyCollection = ResolutionScope.NONE)
public class DecryptConfigValueMojo extends AbstractJenkinsProvisioningMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(required = false, property = "value")
  private String value;

  @Parameter(required = false, property = "prefix")
  private String prefix;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (value != null) {
      logger.info("Decrypted value: {}", crypto().decrypt(value, value));
      return;
    }

    for (File desc : descriptors()) {
      Configuration conf = getConfig(desc);
      String prefix = this.prefix;
      if (prefix == null) {
        prefix = "enc";
      }
      conf = conf.subset(prefix);
      if (!conf.isEmpty()) {
        logger.info("Decrypting " + desc.getName() + "/" + prefix);
        logger.info("");
        for (String key : conf.keySet()) {
          System.out.println(prefix + "." + key + "=" + conf.get(key));
        }
      }
    }
  }

}
