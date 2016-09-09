/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import com.google.common.base.Throwables;

import io.provis.SimpleProvisioner;
import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.jenkins.config.templates.TemplateList;

public class JenkinsConfigurationProvisioner extends SimpleProvisioner {

  public JenkinsConfigurationProvisioner() {
    super();
  }

  public JenkinsConfigurationProvisioner(File localRepository, String remoteRepository) {
    super(localRepository, remoteRepository);
  }

  public MasterConfiguration provision(Configuration configuration, File templateDir, File outputDir) throws IOException {

    Configuration deps = configuration.subset("config.dependencies");
    if (!deps.isEmpty()) {

      List<File> jars = new ArrayList<>();
      for (String v : deps.values()) {
        for (String dep : v.split(",")) {
          dep = dep.trim();
          if (!dep.isEmpty()) {
            jars.add(resolveFromRepository(dep));
          }
        }
      }
      return provisionInClassRealm(jars, configuration, templateDir, outputDir);
    }
    return doProvision(configuration, templateDir, outputDir, null);
  }

  private MasterConfiguration provisionInClassRealm(List<File> jars, Configuration configuration, File templateDir, File outputDir) throws IOException {

    ClassWorld cw = new ClassWorld();
    ClassRealm cr;
    try {
      cr = cw.newRealm("configuration");
    } catch (DuplicateRealmException e) {
      throw Throwables.propagate(e);
    }

    try {

      for (File jar : jars) {
        cr.addURL(jar.toURI().toURL());
      }
      return doProvision(configuration, templateDir, outputDir, cr);

    } finally {
      try {
        cw.disposeRealm(cr.getId());
      } catch (NoSuchRealmException e) {
        throw Throwables.propagate(e);
      }
    }
  }

  protected MasterConfiguration doProvision(Configuration configuration, File templateDir, File outputDir, ClassLoader classLoader) throws IOException {
    MasterConfiguration mc = MasterConfiguration.builder(classLoader)
      .templates(TemplateList.list(templateDir))
      .configuration(configuration)
      .build();
    mc.write(outputDir);
    return mc;
  }

}
