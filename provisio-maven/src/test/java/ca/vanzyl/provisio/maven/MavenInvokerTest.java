package ca.vanzyl.provisio.maven;

import static org.junit.Assert.assertFalse;

import java.io.File;
import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class MavenInvokerTest extends InjectedTest {

    @Inject
    @Named("forked")
    private MavenInvoker maven;

    @Inject
    private MavenInstallationProvisioner provisioner;

    @Inject
    @Named("${basedir}/target/maven")
    private File mavenHome;

    @Inject
    @Named("${basedir}")
    private File basedir;

    @Test
    public void testMavenExecution() throws Exception {
        FileUtils.deleteDirectory(mavenHome.getAbsolutePath());
        File sourceProject = new File(basedir, "src/test/projects/maven");
        File targetProject = new File(basedir, "target/projects/maven");
        FileUtils.deleteDirectory(targetProject);
        FileUtils.copyDirectoryStructure(sourceProject, targetProject);
        provisioner.provision("3.6.3", mavenHome);
        MavenRequest request = new MavenRequest()
                .setMavenHome(mavenHome)
                .addGoals("clean", "install")
                .setWorkDir(targetProject);
        MavenResult result = maven.invoke(request);
        assertFalse(result.hasErrors());
    }
}
