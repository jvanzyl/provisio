/**
 * Copyright (c) 2016 Takari, Inc. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import com.google.common.base.Throwables;

import io.provis.MavenProvisioner;
import io.provis.ProvisioningException;
import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.jenkins.config.templates.TemplateList;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningContext;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;

public class JenkinsConfigurationProvisioner {

  private MavenProvisioner provisioner;

  public JenkinsConfigurationProvisioner(MavenProvisioner provisioner) {
    this.provisioner = provisioner;
  }

  public MasterConfiguration provision(Configuration configuration, File templateDir, File outputDir, boolean writeMasterKey) throws IOException {

    Configuration deps = configuration.subset("config.dependencies");
    if (!deps.isEmpty()) {

      List<File> jars = new ArrayList<>();
      for (String v : deps.values()) {
        for (String dep : v.split(",")) {
          dep = dep.trim();
          if (!dep.isEmpty()) {
            jars.add(resolveJar(dep).getFile());
          }
        }
      }

      return provisionInClassRealm(jars, configuration, templateDir, outputDir, writeMasterKey);
    }
    return doProvision(configuration, templateDir, outputDir, null, writeMasterKey);
  }

  private Artifact resolveJar(String coords) throws IOException {
    ProvisioArtifact art = new ProvisioArtifact(new DefaultArtifact(coords));
    art.addExclusion("*:*");

    ProvisioningRequest preq = new ProvisioningRequest();
    ProvisioningResult pres = new ProvisioningResult(preq);
    ProvisioningContext pctx = new ProvisioningContext(preq, pres);

    Set<ProvisioArtifact> results = provisioner.resolveArtifact(pctx, art);
    if (results.isEmpty()) {
      throw new ProvisioningException("Cannot resolve artifact " + coords);
    }

    return results.iterator().next();
  }

  private MasterConfiguration provisionInClassRealm(List<File> jars, Configuration configuration, File templateDir, File outputDir, boolean writeMasterKey) throws IOException {

    ClassWorld cw = new ClassWorld();
    ClassRealm cr;
    try {
      cr = cw.newRealm("configuration", getClass().getClassLoader());
    } catch (DuplicateRealmException e) {
      throw Throwables.propagate(e);
    }

    try {
      for (File jar : jars) {
        cr.addURL(jar.toURI().toURL());
      }
      return doProvision(configuration, templateDir, outputDir, cr, writeMasterKey);

    } finally {
      try {
        cw.disposeRealm(cr.getId());
      } catch (NoSuchRealmException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  protected MasterConfiguration doProvision(Configuration configuration, File templateDir, File outputDir, ClassLoader classLoader, boolean writeMasterKey) throws IOException {
    MasterConfiguration mc = MasterConfiguration.builder(classLoader).templates(TemplateList.list(templateDir)).configuration(configuration).build();
    mc.write(outputDir, writeMasterKey);
    return mc;
  }

}
