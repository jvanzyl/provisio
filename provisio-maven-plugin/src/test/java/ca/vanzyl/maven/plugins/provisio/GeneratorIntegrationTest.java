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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.6.3", "3.8.4"})
@SuppressWarnings({"JUnitTestNG", "PublicField"})
public class GeneratorIntegrationTest
{
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime maven;

    public GeneratorIntegrationTest(MavenRuntimeBuilder mavenBuilder)
            throws Exception
    {
        this.maven = mavenBuilder.withCliOptions("-B", "-U").build();
    }

    @Test
    public void testBasic()
            throws Exception
    {
        testGenerator("basic");
    }

    @Test
    public void testConflict()
            throws Exception
    {
        File basedir = resources.getBasedir("conflict");
        maven.forProject(basedir)
                .withCliOption("-DdependencyExtendedPomLocation=generated.xml")
                .execute("provisio:generateDependencies")
                .assertLogText("[ERROR] Failed to execute goal ca.vanzyl.provisio.maven.plugins:provisio-maven-plugin:")
                .assertLogText("generateDependencies (default-cli) on project conflict: Found different versions of the same dependency: org.scala-lang:scala-library:jar:2.13.5, org.scala-lang:scala-library:jar:2.13.6 -> [Help 1]");

    }

    protected void testGenerator(String projectId)
            throws Exception
    {
        File basedir = resources.getBasedir(projectId);
        maven.forProject(basedir)
                .withCliOption("-DdependencyExtendedPomLocation=generated.xml")
                .execute("provisio:generateDependencies")
                .assertErrorFreeLog();

        Model model = readModel(new File(basedir, "generated.xml"));
        List<String> dependencies = flattenDependencies(model.getDependencies());

        String[] expected = {
                "org.scala-lang:scala-library:jar:2.13.6:runtime",
                "io.trino:trino-spi:jar:356:provided"};
        assertArrayEquals(expected, dependencies.toArray());
    }


    private Model readModel(File pomFile)
            throws MojoExecutionException
    {
        Reader reader = null;
        try {
            reader = ReaderFactory.newXmlReader(pomFile);
            final Model model = new MavenXpp3Reader().read(reader);
            reader.close();
            return model;
        }
        catch (FileNotFoundException e) {
            return null;
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error reading POM " + pomFile, e);
        }
        catch (XmlPullParserException e) {
            throw new MojoExecutionException("Error parsing POM " + pomFile, e);
        }
        finally {
            IOUtil.close(reader);
        }
    }

    private List<String> flattenDependencies(List<Dependency> dependencies)
    {
        return dependencies
                .stream()
                .map(d -> d.getGroupId() +
                        ":" + d.getArtifactId() +
                        prefixed(d.getType(), ":jar") +
                        prefixed(d.getClassifier(), "") +
                        ":" + d.getVersion() +
                        prefixed(d.getScope(), ":runtime"))
                .collect(Collectors.toList());
    }

    private String prefixed(String value, String defaultValue)
    {
        if (value == null) {
            return defaultValue;
        }
        return ":" + value;
    }
}
