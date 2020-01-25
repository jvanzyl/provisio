Alter/delete entries from archive

``` xml
<runtime>
  <!-- Jenkins -->
  <artifactSet to="/lib">
    <!-- We need to rename as the launcher doesn't pick up the .war extension -->
    <artifact id="org.jenkins-ci.main:jenkins-war:war:1.648" as="jenkins-war-1.648.jar">
      <alter>
        <delete>
          <file path="/WEB-INF/lib/guice-4.0-beta.jar"/> 
        </delete>
      </alter>
    </artifact>
  </artifactSet>
</runtime>
```