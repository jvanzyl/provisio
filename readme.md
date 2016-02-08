# Provisio

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

# References

http://www.unix.com/tips-tutorials/19060-unix-file-permissions.html
