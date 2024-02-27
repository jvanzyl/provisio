package ca.vanzyl.provisio.perms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public final class PosixModesTest {

    @Test(expected = RuntimeException.class)
    public void outOfRangeNumberThrowsIllegalArgumentException() {
        try {
            PosixModes.intModeToPosix(-1);
            shouldHaveThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).isExactlyInstanceOf(RuntimeException.class);
        }

        try {
            PosixModes.intModeToPosix(01000);
            shouldHaveThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).isExactlyInstanceOf(RuntimeException.class);
        }
    }

    @DataProvider
    public static Object[][] intModeTestData() {
        return new Object[][] {
            {0755, "rwxr-xr-x"},
            {0, "---------"},
            {0640, "rw-r-----"},
            {0404, "r-----r--"},
            {0071, "---rwx--x"},
        };
    }

    @Test
    @UseDataProvider("intModeTestData")
    public void translatingToPosixPermissionsWorks(int intMode, String asString) {
        final Set<PosixFilePermission> expected = PosixFilePermissions.fromString(asString);
        assertThat(PosixModes.intModeToPosix(intMode))
                .as("integer mode is correctly translated")
                .isEqualTo(expected);
    }
}
