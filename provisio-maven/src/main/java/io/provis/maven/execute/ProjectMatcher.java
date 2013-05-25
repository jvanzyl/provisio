package io.provis.maven.execute;

import org.codehaus.plexus.util.SelectorUtils;

class ProjectMatcher {

  private final String groupId;

  private final String artifactId;

  private final String version;

  public ProjectMatcher(String projectId) {
    String coords[] = parse(projectId);
    groupId = (coords[0].length() > 0) ? coords[0] : "*";
    artifactId = (coords[1].length() > 0) ? coords[1] : "*";
    version = (coords[2].length() > 0) ? coords[2] : "*";
  }

  private String[] parse(String projectId) {
    String[] parts = projectId.split("\\:");
    String[] coords;
    if (parts.length == 1) {
      // artifactId
      coords = new String[] {
          "", parts[0], ""
      };
    } else if (parts.length == 2) {
      // groupId:artifactId
      coords = new String[] {
          parts[0], parts[1], ""
      };
    } else if (parts.length == 3) {
      // groupId:artifactId:version
      coords = new String[] {
          parts[0], parts[1], parts[2]
      };
    } else {
      throw new IllegalArgumentException("Bad project identifier " + projectId + ", expected format is <groupId>:<artifactId>[:<version>]");
    }
    return coords;
  }

  public boolean isMatch(String projectId) {
    String coords[] = parse(projectId);
    if (!SelectorUtils.match(groupId, coords[0])) {
      return false;
    }
    if (!SelectorUtils.match(artifactId, coords[1])) {
      return false;
    }
    if (!SelectorUtils.match(version, coords[2])) {
      return false;
    }
    return true;
  }

}
