package io.provis.parser;

public class StringUtils {
  public static int countMatches(String str, String sub) {
    if (isEmpty(str) || isEmpty(sub)) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

}
