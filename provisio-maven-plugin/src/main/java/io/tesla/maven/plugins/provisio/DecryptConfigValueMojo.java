/**
 * Copyright (c) 2017 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.jenkins.crypto.ConfigCrypto;

@Mojo(name = "decrypt-config", requiresProject = false, requiresDependencyCollection = ResolutionScope.NONE)
public class DecryptConfigValueMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(required = false, property = "encryptionKey")
  private String encryptionKey;

  @Parameter(required = false, property = "value")
  private String value;

  public void execute() throws MojoExecutionException, MojoFailureException {
    logger.info("Decrypted value: {}", new ConfigCrypto(encryptionKey).decrypt(value, value));
  }

}
