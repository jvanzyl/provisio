package com.sonatype.provisio.scm;

class ScmUriUtils {

  public static final String GIT = "git";

  public static final String SVN = "svn";

  public static final String CVS = "cvs";

  public static String guessScmType(String uri) {
    if (uri.endsWith(".git") || uri.endsWith(".git/") || uri.contains("github.com") || uri.contains("git:")) {
      return GIT;
    }
    if (uri.contains("svn.") || uri.contains("svn:") || uri.contains("svn+")) {
      return SVN;
    }
    if (uri.contains("cvs.") || uri.contains("pserver")) {
      return CVS;
    }
    return "";
  }

}
