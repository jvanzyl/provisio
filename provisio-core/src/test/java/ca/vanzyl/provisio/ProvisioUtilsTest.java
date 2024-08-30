package ca.vanzyl.provisio;

import static ca.vanzyl.provisio.ProvisioUtils.coordinateToPath;
import static ca.vanzyl.provisio.ProvisioUtils.targetArtifactFileName;
import static org.junit.Assert.assertEquals;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import org.junit.Test;

public class ProvisioUtilsTest {

    @Test
    public void validateCoordinateToPath() {

        ProvisioArtifact artifact = new ProvisioArtifact("io.trinodb:trino-main:445");
        String path = coordinateToPath(artifact);
        assertEquals("trino-main-445.jar", path);
    }

    @Test
    public void validateArtifactFilenames() {
        assertEquals(
                "io.trinodb_trino.jar",
                targetArtifactFileName(new ProvisioArtifact("io.trinodb:trino-main:445"), "trino.jar"));
        assertEquals(
                "io.trinodb_trino-main-445.jar",
                targetArtifactFileName(new ProvisioArtifact("io.trinodb:trino-main:455"), "trino-main-445.jar"));
        assertEquals(
                "org.wso2.carbon.identity.user.s....configuration.stub-7.4.15.jar",
                targetArtifactFileName(
                        new ProvisioArtifact(
                                "org.wso2.carbon.identity.framework:org.wso2.carbon.identity.user.store.configuration.stub:7.4.15"),
                        "org.wso2.carbon.identity.user.store.configuration.stub-7.4.15.jar"));
        assertEquals(
                "org.wso2.ca...y.framework_org.wso2.carbon.user.mgt.ui-7.4.15.jar",
                targetArtifactFileName(
                        new ProvisioArtifact("org.wso2.carbon.identity.framework:org.wso2.carbon.user.mgt.ui:7.4.15"),
                        "org.wso2.carbon.user.mgt.ui-7.4.15.jar"));
    }
}
