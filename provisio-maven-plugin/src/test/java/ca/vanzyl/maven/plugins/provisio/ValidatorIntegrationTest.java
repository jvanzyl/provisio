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

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.3.9", "3.5.4", "3.6.3", "3.8.1"})
@SuppressWarnings({"JUnitTestNG", "PublicField"})
public class ValidatorIntegrationTest
{
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public ValidatorIntegrationTest(MavenRuntimeBuilder mavenBuilder)
            throws Exception
    {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testIncomplete()
            throws Exception
    {
        File basedir = resources.getBasedir("basic");

        maven.forProject(basedir)
                .execute("provisio:validateDependencies")
                .assertLogText("[ERROR] Failed to execute goal ca.vanzyl.provisio.maven.plugins:provisio-maven-plugin:")
                .assertLogText("validateDependencies (default-cli) on project basic: Missing dependencies: junit:junit:jar:4.13.2 -> [Help 1]");
    }

    @Test
    public void testComplete()
            throws Exception
    {
        File basedir = resources.getBasedir("complete");
        maven.forProject(basedir)
                .execute("provisio:validateDependencies")
                .assertErrorFreeLog();
    }
}
