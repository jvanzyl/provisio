# Provisio

A Maven-based provisioning mechanism and replacement for the maven-assembly-plugin.

- Support for building runtimes, like the maven-assembly-plug, that are aware of their dependencies in a multi-module build
- Support for hardlinking in TAR archives with corresponding support for dereferencing hardlinks when unpacking archives
- Support for excluding artifacts while resolving a specific artifact
- Support for globally excluding artifacts while transitively resolving artifacts
- Support for filtering resources while unpacking archives
- Support for [Mustache](https://github.com/spullara/mustache.java) filtering resources while unpacking archives
- Support for deleting an artifact out of an archive on the fly
- Support for inserting artifacts into archives on the fly
- Support for standard addition of files to a runtime
- Support for automatic exclusions in parent/child artifactSet relationships

## Support for building runtimes

```
<runtime>
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

  <!-- Webserver -->
  <artifactSet to="/lib">
    <artifact id="io.takari:webserver:0.0.2-SNAPSHOT">
    </artifact>
  </artifactSet>

  <!-- Webapps -->
  <artifactSet to="/webapps">
    <artifact id="org.jenkins-ci.main:jenkins-war:war:1.647">
    </artifact>
  </artifactSet>
</runtime>
```

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

# References

http://www.unix.com/tips-tutorials/19060-unix-file-permissions.html
