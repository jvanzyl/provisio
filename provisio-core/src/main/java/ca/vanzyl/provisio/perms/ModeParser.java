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
package ca.vanzyl.provisio.perms;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A parser for chmod-like posix mode change instructions
 *
 * <p>Such instructions are, for instance, {@code "u+r,go-w"}.</p>
 *
 * <p>What is currently supported:</p>
 *
 * <ul>
 *     <li>who: any combination of {@code u}, {@code g} and {@code o}; an empty
 *     string is equivalent to {@code ugo}; {@code a} is NOT supported;</li>
 *     <li>operation type: {@code +} and {@code -}; {@code =} is NOT supported;
 *     </li>
 *     <li>what: any combination of {@code r}, {@code w} and {@code x}; {@code
 *     X} is NOT supported.</li>
 * </ul>
 */
public final class ModeParser {
  private static final Pattern COMMA = Pattern.compile(",");

  private ModeParser() {
    throw new Error("nice try!");
  }

  /**
   * Build a permission change object from an instruction string
   *
   * @param instructions the instructions
   * @return a permission change object
   * @throws InvalidModeInstructionException instruction string is invalid
   * @throws UnsupportedOperationException an unsupported instruction was
   * encountered while parsin
   */
  public static PermissionsSet buildPermissionsSet(final String instructions) {
    Objects.requireNonNull(instructions);

    final Set<PosixFilePermission> toAdd = EnumSet.noneOf(PosixFilePermission.class);
    final Set<PosixFilePermission> toRemove = EnumSet.noneOf(PosixFilePermission.class);

    parse(instructions, toAdd, toRemove);

    return new PermissionsSet(toAdd, toRemove);
  }

  // Visible for testing
  static void parse(final String instructions,
    final Set<PosixFilePermission> toAdd,
    final Set<PosixFilePermission> toRemove) {
    for (final String instruction : COMMA.split(instructions)) {
      parseOne(instruction, toAdd, toRemove);
    }
  }

  // Visible for testing
  static void parseOne(final String instruction,
    final Set<PosixFilePermission> toAdd,
    final Set<PosixFilePermission> toRemove) {
    final int plus = instruction.indexOf('+');
    final int minus = instruction.indexOf('-');
    if (plus < 0 && minus < 0) {
      throw new RuntimeException("Invalid unix mode expresson: '" + instruction + "'");
    }
    final String who;
    final String what;
    final Set<PosixFilePermission> set;
    if (plus >= 0) {
      who = plus == 0 ? "ugo" : instruction.substring(0, plus);
      what = instruction.substring(plus + 1);
      set = toAdd;
    } else {
      // If it's not plusIndex it's minusIndex
      who = minus == 0 ? "ugo" : instruction.substring(0, minus);
      what = instruction.substring(minus + 1);
      set = toRemove;
    }
    if (what.isEmpty()) {
      throw new RuntimeException("Invalid unix mode expresson: '" + instruction + "'");
    }
    modifySet(who, what, set, instruction);
  }

  private static void modifySet(final String who, final String what,
    final Set<PosixFilePermission> set, final String instruction) {
    final int whoLength = who.length();
    final int whatLength = what.length();
    int whoOrdinal, whatOrdinal;
    for (int i = 0; i < whoLength; i++) {
      whoOrdinal = 0;
      switch (who.charAt(i)) {
        case 'o':
          whoOrdinal++;
          /* fall through */
        case 'g':
          whoOrdinal++;
          /* fall through */
        case 'u':
          break;
        case 'a':
          throw new UnsupportedOperationException(instruction);
        default:
          throw new RuntimeException("Invalid unix mode expresson: '" + instruction + "'");
      }
      for (int j = 0; j < whatLength; j++) {
        whatOrdinal = 3 * whoOrdinal;
        switch (what.charAt(j)) {
          case 'x':
            whatOrdinal++;
            /* fall through */
          case 'w':
            whatOrdinal++;
            /* fall through */
          case 'r':
            break;
          case 'X':
            throw new UnsupportedOperationException(instruction);
          default:
            throw new RuntimeException("Invalid unix mode expresson: '" + instruction + "'");
        }
        //add to set
        set.add(PosixModes.PERMISSIONS[whatOrdinal]);
      }
    }
  }
}
