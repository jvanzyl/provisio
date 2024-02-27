package ca.vanzyl.provisio.perms;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public final class ModeParserTest {

    private static final Set<PosixFilePermission> NO_PERMISSIONS = EnumSet.noneOf(PosixFilePermission.class);

    @DataProvider
    public static Object[] invalidModeInstructions() {
        return new Object[] {
            "", "ur", "u+", "t+r", "uw-r", "u-y",
        };
    }

    @Test
    @UseDataProvider(value = "invalidModeInstructions")
    public void invalidModeInstructionThrowsAppropriateException(final String instruction) {
        final Set<PosixFilePermission> toAdd = EnumSet.noneOf(PosixFilePermission.class);
        final Set<PosixFilePermission> toRemove = EnumSet.noneOf(PosixFilePermission.class);
        try {
            ModeParser.parseOne(instruction, toAdd, toRemove);
            shouldHaveThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            assertThat(e)
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage(String.format("Invalid unix mode expresson: '%s'", instruction));
        }
    }

    @Test
    public void unsupportedModeInstructionThrowsAppropriateException() {
        final Set<PosixFilePermission> toAdd = EnumSet.noneOf(PosixFilePermission.class);
        final Set<PosixFilePermission> toRemove = EnumSet.noneOf(PosixFilePermission.class);
        String instruction;
        instruction = "u-X";
        try {
            ModeParser.parseOne(instruction, toAdd, toRemove);
            shouldHaveThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException e) {
            assertThat(e)
                    .isExactlyInstanceOf(UnsupportedOperationException.class)
                    .hasMessage(instruction);
        }
        instruction = "a+r";
        try {
            ModeParser.parseOne(instruction, toAdd, toRemove);
            shouldHaveThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException e) {
            assertThat(e)
                    .isExactlyInstanceOf(UnsupportedOperationException.class)
                    .hasMessage(instruction);
        }
    }

    @DataProvider
    public static Object[][] validModeInstructions() {
        return new Object[][] {
            {"ug+r", EnumSet.of(OWNER_READ, GROUP_READ), NO_PERMISSIONS},
            {"ug-r", NO_PERMISSIONS, EnumSet.of(OWNER_READ, GROUP_READ)},
            {"o-x", NO_PERMISSIONS, EnumSet.of(OTHERS_EXECUTE)},
            {"uog-rxw", NO_PERMISSIONS, EnumSet.allOf(PosixFilePermission.class)}
        };
    }

    @Test
    @UseDataProvider("validModeInstructions")
    public void validModeInstructionAddsInstructionsInAppropriateSets(
            final String instruction, final Set<PosixFilePermission> add, final Set<PosixFilePermission> remove) {
        final Set<PosixFilePermission> toAdd = EnumSet.noneOf(PosixFilePermission.class);
        final Set<PosixFilePermission> toRemove = EnumSet.noneOf(PosixFilePermission.class);
        ModeParser.parseOne(instruction, toAdd, toRemove);
        assertThat(toAdd).isEqualTo(add);
        assertThat(toRemove).isEqualTo(remove);
    }

    @DataProvider
    public static Object[][] multipleInstructions() {
        return new Object[][] {
            {"ug+r,ou-x", EnumSet.of(OWNER_READ, GROUP_READ), EnumSet.of(OTHERS_EXECUTE, OWNER_EXECUTE)},
            {
                "ug-r,og+xr",
                EnumSet.of(OTHERS_EXECUTE, OTHERS_READ, GROUP_EXECUTE, GROUP_READ),
                EnumSet.of(OWNER_READ, GROUP_READ)
            }
        };
    }

    @Test
    @UseDataProvider("multipleInstructions")
    public void validModeMultipleInstructionAddsInstructionsInAppropriateSets(
            final String instruction, final Set<PosixFilePermission> add, final Set<PosixFilePermission> remove) {
        final Set<PosixFilePermission> toAdd = EnumSet.noneOf(PosixFilePermission.class);
        final Set<PosixFilePermission> toRemove = EnumSet.noneOf(PosixFilePermission.class);
        ModeParser.parse(instruction, toAdd, toRemove);
        assertThat(toAdd).isEqualTo(add);
        assertThat(toRemove).isEqualTo(remove);
    }
}
