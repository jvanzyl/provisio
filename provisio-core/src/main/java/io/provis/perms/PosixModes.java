package io.provis.perms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

public final class PosixModes {
  static final PosixFilePermission[] PERMISSIONS = PosixFilePermission.values();

  private static final int PERMISSIONS_LENGTH = PERMISSIONS.length;
  private static final int INT_MODE_MAX = (1 << PERMISSIONS_LENGTH) - 1;

  private PosixModes() {
    throw new Error("nice try!");
  }

  /**
   * Convert an integer into a set of {@link PosixFilePermission}s
   *
   * <p>Note that this method will not try and read {@code 755} "in octal";
   * you <strong>must</strong> prefix your integer with {@code 0} so that the
   * constant be octal, as in {@code 0755}.</p>
   *
   * @param intMode the mode
   * @return a set of POSIX permissions
   * @throws InvalidIntModeException invalid integer mode
   *
   * @see Files#setPosixFilePermissions(Path, Set)
   */
  public static Set<PosixFilePermission> intModeToPosix(int intMode) {
    if ((intMode & INT_MODE_MAX) != intMode) {
      throw new RuntimeException("Invalid intMode: " + intMode);
    }
    final Set<PosixFilePermission> set = EnumSet.noneOf(PosixFilePermission.class);

    for (int i = 0; i < PERMISSIONS_LENGTH; i++) {
      if ((intMode & 1) == 1) {
        set.add(PERMISSIONS[PERMISSIONS_LENGTH - i - 1]);
      }
      /*
       * We're OK with >> instead of >>>, the sign bit will never be set
       */
      intMode >>= 1;
    }
    return set;
  }
}
