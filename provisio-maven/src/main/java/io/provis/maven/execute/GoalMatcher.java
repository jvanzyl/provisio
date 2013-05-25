package io.provis.maven.execute;

import java.util.Collection;

import org.codehaus.plexus.util.SelectorUtils;

class GoalMatcher {

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String goal;

  private final String exec;

  public GoalMatcher(String goalId) {
    String coords[] = parse(goalId);
    groupId = (coords[0].length() > 0) ? coords[0] : "*";
    artifactId = (coords[1].length() > 0) ? coords[1] : "*";
    version = (coords[2].length() > 0) ? coords[2] : "*";
    goal = (coords[3].length() > 0) ? coords[3] : "*";
    exec = (coords[4].length() > 0) ? coords[4] : "*";
  }

  private String[] parse(String goalId) {
    String[] parts = goalId.split("\\:");
    String[] coords;
    if (parts.length == 1) {
      // artifactId
      coords = new String[] {
          "", parts[0], "", "", ""
      };
    } else if (parts.length == 2) {
      // artifactId:goal
      coords = new String[] {
          "", parts[0], "", parts[1], ""
      };
    } else if (parts.length == 3) {
      // groupId:artifactId:goal
      coords = new String[] {
          parts[0], parts[1], "", parts[2], ""
      };
    } else if (parts.length == 4) {
      // groupId:artifactId:goal:executionId
      coords = new String[] {
          parts[0], parts[1], "", parts[2], parts[3]
      };
    } else if (parts.length == 5) {
      // groupId:artifactId:version:goal:executionId
      coords = new String[] {
          parts[0], parts[1], parts[2], parts[3], parts[4]
      };
    } else {
      throw new IllegalArgumentException("Bad goal identifier " + goalId + ", expected format is [<groupId>:]<artifactId>[:<version>]:<goal>[:<executionId>]");
    }
    return coords;
  }

  public boolean isMatch(String goalId) {
    String coords[] = parse(goalId);
    if (!SelectorUtils.match(groupId, coords[0])) {
      return false;
    }
    if (!SelectorUtils.match(artifactId, coords[1])) {
      return false;
    }
    if (!SelectorUtils.match(version, coords[2])) {
      return false;
    }
    if (!SelectorUtils.match(goal, coords[3])) {
      return false;
    }
    if (!SelectorUtils.match(exec, coords[4])) {
      return false;
    }
    return true;
  }

  public boolean hasMatch(Collection<String> goalIds) {
    for (String goalId : goalIds) {
      if (isMatch(goalId)) {
        return true;
      }
    }
    return false;
  }

}
