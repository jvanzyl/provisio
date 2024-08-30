/**
 * Copyright (C) 2015-2020 Jason van Zyl
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.maven.plugins.provisio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.6.3", "3.8.8", "3.9.6"})
@SuppressWarnings({"JUnitTestNG", "PublicField"})
public class ProvisioningIntegrationTest {
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public ProvisioningIntegrationTest(MavenRuntimeBuilder mavenBuilder) throws Exception {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testTransitiveWithLocalTestScope() throws Exception {
        File basedir = resources.getBasedir("transitive-test");
        maven.forProject(basedir)
                .withCliOption("-X")
                .execute("provisio:provision")
                .assertErrorFreeLog();

        File libdir = new File(basedir, "target/test-1.0/lib");
        assertTrue("guice exists", new File(libdir, "com.google.inject_guice-7.0.0.jar").isFile());
        assertTrue("guava exists", new File(libdir, "com.google.guava_guava-31.0.1-jre.jar").isFile());
        assertFalse("slf4j-api not exists", new File(libdir, "org.slf4j_slf4j-api-2.0.11.jar").isFile());
        assertTrue("slf4j-simple exists", new File(libdir, "org.slf4j_slf4j-simple-2.0.11.jar").isFile());
    }

    @Test
    public void testConflictingArtifacts() throws Exception {
        File basedir = resources.getBasedir("conflicting-filenames");
        maven.forProject(basedir)
                .withCliOption("-X")
                .execute("provisio:provision")
                .assertErrorFreeLog();

        File libdir = new File(basedir, "target/test-1.0/lib");
        assertTrue(
                "io.opentelemetry:opentelemetry-semconv exists",
                new File(libdir, "io.opentelemetry_opentelemetry-semconv-1.27.0-alpha.jar").isFile());
        assertTrue(
                "io.opentelemetry.semconv:opentelemetry-semconv exists too",
                new File(libdir, "io.opentelemetry.semconv_opentelemetry-semconv-1.27.0-alpha.jar").isFile());
    }
}
