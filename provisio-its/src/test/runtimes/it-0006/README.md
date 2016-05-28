Exclude transitive artifacts from being resolved

``` xml
<runtime>
  <!-- Jenkins -->
  <artifactSet to="/lib">
    <artifact id="org.apache.maven:maven-core:3.3.9">
      <exclude id="org.codehaus.plexus:plexus-utils"/>
    </artifact>
  </artifactSet>
</runtime>
```