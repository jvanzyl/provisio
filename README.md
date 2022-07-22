# Provisio

A Maven-based provisioning mechanism and replacement for the maven-assembly-plugin.

- [Support for building runtimes, like the maven-assembly-plugin, that are aware of their dependencies in a multi-module build](#feature0)
- [Support for hardlinking in TAR archives with corresponding support for dereferencing hardlinks when unpacking archives](#feature1)
- [Support for excluding artifacts while resolving a specific artifact](#feature2)
- [Support for globally excluding artifacts while transitively resolving artifacts](#feature3)
- [Support for filtering resources while unpacking archives](#feature4)
- [Support for Mustache filtering resources while unpacking archives](#feature5)
- [Support for deleting an artifact out of an archive on the fly](#feature6)
- [Support for inserting artifacts into archives on the fly](#feature7)
- [Support for standard addition of files to a runtime](#feature8)
- [Support for automatic exclusions in parent/child artifactSet relationships](#feature9)
- [Support for generating dependencies for pom.xml](#feature10)

Provisio was originally created for the [Presto](https://prestosql.io/) project to provide a common way to build [Presto plugins](https://github.com/prestosql/presto-maven-plugin) and build the [Presto Server](https://github.com/prestosql/presto/tree/master/presto-server). You'll notice how small the Presto Server [`pom.xml`](https://github.com/prestosql/presto/blob/master/presto-server/pom.xml) is even though, at the time of this writing, there are 30+ plugins packaged in the Presto Server build as per the [Provisio descriptor](https://github.com/prestosql/presto/blob/master/presto-server/src/main/provisio/presto.xml). As you'll read below, you only need to specify what you need in the Provisio descriptor and Maven will figure out the rest, correctly, without having to pollute your `pom.xml` with duplicated dependendency declarations.

<a name="feature0"></a>
## Support for building runtimes

To use Provisio declare its use in your project's `pom.xml` and be sure to set the packaging to `provisio` and enable the plugin as an extension. Provisio implements a Maven `LifecycleParticipant` that inspects all the artifacts used in your runtime descriptor and will order your build accordingly. You might notice below the conspicuous lack of a dependencies section: you do not need to include a dependency in your `pom.xml` for an artifact produced in the current build to have it be included in your runtime. Provisio will find all the artifact references in your runtime descriptor, determine the correct build ordering, and instruct Maven to make the necessary changes.

```
<project>
  <groupId>ca.vanzyl.ollie</groupId>
  <artifacId>ollie-server</artifacId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>provisio</packaging>

  <build>
    <plugins>
      <plugin>
        <groupId>ca.vanzyl.provisio.maven.plugins</groupId>
        <artifactId>provisio-maven-plugin</artifactId>
        <version>1.0.7</version>
        <extensions>true</extensions>
        <configuration>
          <outputDirectory>${project.build.directory}/presto-server-${project.version}</outputDirectory>
        </configuration>      
      </plugin>
    </plugins>
  </build>
</project>
```

Below we are constructing a runtime that has some legal notices, a launcher, some libraries, and plugins. To reiterate you do not need to specify any of the artifact dependencies in your `pom.xml`. Provisio will see that you have `ca.vanzyl.ollie:plugin-one:${project.version}` in the descriptor and knows this artifact is present in your multi-module build. Provisio will make sure Maven builds `ca.vanzyl.ollie:plugin-one:${project.version}` first so that it can be packaged as part of this runtime build.

```
<runtime>
  <!-- Provisio determines what archive type it's dealing with by the extension tar.gz here -->
  <archive name="${project.artifactId}-${project.version}.tar.gz" />

  <!-- Notices -->
  <fileSet to="/">
    <directory path="${basedir}">
      <include>NOTICE</include>
      <include>README.txt</include>
    </directory>
  </fileSet>

  <!-- Launcher -->
  <artifactSet to="/bin">
    <artifact id="io.airlift:launcher:tar.gz:bin:0.124">
      <unpack />
    </artifact>
    <artifact id="io.airlift:launcher:tar.gz:properties:0.124">
      <unpack filter="true" />
    </artifact>
  </artifactSet>

  <fileSet to="/etc">
    <directory path="${basedir}/src/main/etc"/>
  </fileSet>

  <!-- Main -->
  <artifactSet to="/lib">
    <artifact id="ca.vanzyl.ollie:ollie-main:${project.version}">
    </artifact>
  </artifactSet>

  <!-- Plugins -->
  <artifactSet to="/plugins/one">
    <artifact id="ca.vanzyl.ollie:plugin-one:${project.version}">
    </artifact>
  </artifactSet>

  <artifactSet to="/plugins/two">
    <artifact id="ca.vanzyl.ollie:plugin-two:${project.version}">
    </artifact>
  </artifactSet>

</runtime>
```

Provisio descriptors are searched for in `src/main/provisio` and the runtime descriptor can be named anything provided it has an `.xml` extension. So `src/main/provisio/server.xml` or `src/main/provisio/runtime.xml` would be perfectly suitable names for your runtime descriptor.

What follows are various techniques and capabilities for building runtimes. Provisio is very good at working with the zip and tar.gz formats, and very good at manipulating Maven artifacts and sets of Maven artifacts.

<a name="feature1"></a>
## Hardlinking in TAR archives

```
<runtime>

  <!-- Produce the archive of this runtime with duplicate JARS hardlinked -->
  <archive name="presto-server-${project.version}.tar.gz" hardLinkIncludes="**/*.jar" />

  <!-- Dereference hardlinks in tar.gz and exclude a directory on the fly -->
  <artifactSet to="/">
    <artifact id="io.prestosql:presto-server:tar.gz:${dep.presto.version}">
      <unpack dereferenceHardlinks="true" useRoot="false" />
      <exclude dir="plugin/hive-hadoop2" />
    </artifact>
  </artifactSet>
</runtime>

```

<a name="feature2"></a>
## Excluding artifacts while resolving a specific artifact

```
<runtime>
  <artifactSet to="/lib">
    <artifact id="org.apache.maven:maven-core:3.3.9">
      <exclusion id="org.codehaus.plexus:plexus-utils"/>
      <exclusion id="org.apache.maven:maven-model"/>      
    </artifact>
  </artifactSet>
</runtime>
```

<a name="feature3"></a>
## Globally excluding artifacts while transitively resolving artifacts

```
<runtime>
  <artifactSet to="/lib">
    <exclusion id="org.codehaus.plexus:plexus-utils"/>
    <exclusion id="org.apache.maven:maven-model"/>          
    <artifact id="org.codehaus.modello:modello-core:1.8.3"/>
    <artifact id="org.apache.maven:maven-core:3.3.9"/>
  </artifactSet>
</runtime>
```

<a name="feature4"></a>
## Filtering resources while unpacking archives

```
<runtime>
  <artifactSet to="/">
    <artifact id="ca.vanzyl:archive-with-resources-to-filter:1.0.0">
      <unpack useRoot="true" filtering="true" />
    </artifact>
  </artifactSet>
</runtime>
```

<a name="feature5"></a>
## Mustache filtering resources while unpacking archives


```
<runtime>
  <artifactSet to="/">
    <artifact id="ca.vanzyl:archive-with-mustache-templates:1.0.0">
      <unpack useRoot="true" mustache="true" />
    </artifact>
  </artifactSet>
</runtime>
```

<a name="feature6"></a>
## Deleting an artifact out of an archive on the fly

```
<runtime>
  <artifactSet to="/lib">
    <artifact id="org.eclipse.hudson:hudson-war:war:3.3.3" as="hudson-war-3.3.3.jar">
      <alter>
        <delete>
          <file path="/WEB-INF/lib/hudson-core-3.3.3.jar"/>
        </delete>
      </alter>
    </artifact>
  </artifactSet>
</runtime>
```

<a name="feature7"></a>
## Inserting artifacts into archives on the fly

```
<runtime>
  <artifactSet to="/lib">
    <artifact id="org.eclipse.hudson:hudson-war:war:3.3.3" as="hudson-war-3.3.3.jar">
      <alter>
        <insert>
          <artifact
            id="junit:junit:4.12"
            as="/WEB-INF/lib/junit-4.12.jar"/>
        </insert>
      </alter>
    </artifact>
  </artifactSet>
</runtime>
```

<a name="feature8"></a>
## Standard addition of files to a runtime

```
<runtime>
  <fileSet to="/">
    <directory path="${basedir}">
      <include>LICENSE.txt</include>
    </directory>
  </fileSet>

  <fileSet to="/etc">
    <file path="${basedir}/src/main/etc/jvm.config"/>
    <file path="${basedir}/src/main/etc/config.properties"/>
    <directory path="${basedir}/src/main/etc">
      <include>**/*.properties</include>
    </directory>
  </fileSet>
</runtime>
```

<a name="feature9"></a>
## Automatic exclusions in parent/child artifactSet relationships

```
<runtime>    
  <!--
   | Logback classic >> Logback core >> SLF4J API so we expect the following
   | shape:
   |
   | lib/
   |   logback-classic
   |   ext/
   |     logback-core
   |     slf4j-api
   |
   | In this case we expect logback core to act as an exclude in the parent
   | artifact set so that it will only be resolve in the child artifact set.
   -->
  <artifactSet to="lib">
    <artifact id="ch.qos.logback:logback-classic:${logbackVersion}"/>
    <artifactSet to="ext">
      <artifact id="ch.qos.logback:logback-core:${logbackVersion}"/>
    </artifactSet>
  </artifactSet>
</runtime>
```

<a name="feature10"></a>
## Generating dependencies

If you do need all the dependencies to be defined in `pom.xml`, for example to use other Maven plugins that process them, use the `generate` Mojo to avoid having to manage them manually:
```
mvn provisio:generateDependencies -DdependencyExtendedPomLocation=pom-generated.xml
```

Without `-DpomFile=pom-generated.xml`, dependencies would be added to the existing `pom.xml`, but it won't preserve any comments.

To only check if the `pom.xml` file is already up to date, run:
```
mvn provisio:verifyDependencies
```

# References

http://www.unix.com/tips-tutorials/19060-unix-file-permissions.html

[mustache]: https://github.com/spullara/mustache.java
