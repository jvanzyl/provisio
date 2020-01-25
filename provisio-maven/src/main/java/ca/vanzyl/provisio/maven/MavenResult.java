/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class MavenResult {

  private String output;
  private Map<String, List<String>> executedGoals;
  private Collection<String> resolvedArtifacts;
  private Collection<String> deployedArtifacts;
  private List<Throwable> errors;

  public String getOutput() {
    return (output != null) ? output : "";
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public Map<String, List<String>> getExecutedGoals() {
    if (executedGoals == null) {
      executedGoals = new HashMap<String, List<String>>();
    }
    return executedGoals;
  }

  public void setExecutedGoals(Map<String, List<String>> executedGoals) {
    this.executedGoals = executedGoals;
  }

  public Collection<String> getResolvedArtifacts() {
    if (resolvedArtifacts == null) {
      resolvedArtifacts = new LinkedHashSet<String>();
    }
    return resolvedArtifacts;
  }

  public void setResolvedArtifacts(Collection<String> resolvedArtifacts) {
    this.resolvedArtifacts = resolvedArtifacts;
  }

  public Collection<String> getDeployedArtifacts() {
    if (deployedArtifacts == null) {
      deployedArtifacts = new LinkedHashSet<String>();
    }
    return deployedArtifacts;
  }

  public void setDeployedArtifacts(Collection<String> deployedArtifacts) {
    this.deployedArtifacts = deployedArtifacts;
  }

  public List<Throwable> getErrors() {
    if (errors == null) {
      errors = new ArrayList<Throwable>();
    }
    return errors;
  }

  public void setErrors(List<Throwable> errors) {
    this.errors = errors;
  }

  public boolean hasErrors() {
    return errors != null;
  }

}
