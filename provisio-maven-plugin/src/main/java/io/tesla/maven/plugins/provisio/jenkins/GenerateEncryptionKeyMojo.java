/**
 * Copyright (c) 2017 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio.jenkins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.jenkins.crypto.ConfigCrypto;

@Mojo(name = "gen-encryption-key", requiresProject = false, requiresDependencyCollection = ResolutionScope.NONE)
public class GenerateEncryptionKeyMojo extends AbstractJenkinsProvisioningMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(required = false, defaultValue = "128", property = "keyLength")
  private int keyLength;

  public void execute() throws MojoExecutionException, MojoFailureException {
    logger.info("Generating {}bit key: {}", keyLength, ConfigCrypto.generateEncryptionKey(keyLength));
  }

}
