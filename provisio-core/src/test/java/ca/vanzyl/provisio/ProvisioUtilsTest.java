package ca.vanzyl.provisio;

import static ca.vanzyl.provisio.ProvisioUtils.coordinateToPath;
import static ca.vanzyl.provisio.ProvisioUtils.targetArtifactFileName;
import static org.junit.Assert.assertEquals;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import java.util.HashMap;
import org.junit.Test;

public class ProvisioUtilsTest {

    @Test
    public void validateCoordinateToPath() {

        ProvisioArtifact artifact = new ProvisioArtifact("io.trinodb:trino-main:445");
        String path = coordinateToPath(artifact);
        assertEquals("trino-main-445.jar", path);
    }

    @Test
    public void validateArtifactFilenames_A() {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setVariables(new HashMap<>());
        ProvisioningResult result = new ProvisioningResult(request);
        ProvisioningContext context = new ProvisioningContext(request, result);
        assertEquals(
                "trino.jar",
                targetArtifactFileName(context, new ProvisioArtifact("io.trinodb:trino-main:445"), "trino.jar"));
        assertEquals(
                "trino-main-445.jar",
                targetArtifactFileName(
                        context, new ProvisioArtifact("io.trinodb:trino-main:455"), "trino-main-445.jar"));
        assertEquals(
                "org.wso2.carbon.identity.user.store.configuration.stub-7.4.15.jar",
                targetArtifactFileName(
                        context,
                        new ProvisioArtifact(
                                "org.wso2.carbon.identity.framework:org.wso2.carbon.identity.user.store.configuration.stub:7.4.15"),
                        "org.wso2.carbon.identity.user.store.configuration.stub-7.4.15.jar"));
        assertEquals(
                "org.wso2.carbon.user.mgt.ui-7.4.15.jar",
                targetArtifactFileName(
                        context,
                        new ProvisioArtifact("org.wso2.carbon.identity.framework:org.wso2.carbon.user.mgt.ui:7.4.15"),
                        "org.wso2.carbon.user.mgt.ui-7.4.15.jar"));
    }

    @Test
    public void validateArtifactFilenames_GA() {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setVariables(new HashMap<>());
        request.getVariables()
                .put(
                        ProvisioVariables.FALLBACK_TARGET_FILE_NAME_MODE,
                        ProvisioVariables.FallBackTargetFileNameMode.GA.name());
        ProvisioningResult result = new ProvisioningResult(request);
        ProvisioningContext context = new ProvisioningContext(request, result);
        assertEquals(
                "io.trinodb_trino.jar",
                targetArtifactFileName(context, new ProvisioArtifact("io.trinodb:trino-main:445"), "trino.jar"));
        assertEquals(
                "io.trinodb_trino-main-445.jar",
                targetArtifactFileName(
                        context, new ProvisioArtifact("io.trinodb:trino-main:455"), "trino-main-445.jar"));
        assertEquals(
                "org.wso2.carbon.identity.user.s....configuration.stub-7.4.15.jar",
                targetArtifactFileName(
                        context,
                        new ProvisioArtifact(
                                "org.wso2.carbon.identity.framework:org.wso2.carbon.identity.user.store.configuration.stub:7.4.15"),
                        "org.wso2.carbon.identity.user.store.configuration.stub-7.4.15.jar"));
        assertEquals(
                "org.wso2.ca...y.framework_org.wso2.carbon.user.mgt.ui-7.4.15.jar",
                targetArtifactFileName(
                        context,
                        new ProvisioArtifact("org.wso2.carbon.identity.framework:org.wso2.carbon.user.mgt.ui:7.4.15"),
                        "org.wso2.carbon.user.mgt.ui-7.4.15.jar"));
    }
}
