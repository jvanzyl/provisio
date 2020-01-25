Exclude transitive artifacts from being resolved in an artifact set

``` xml
<runtime>
  <artifactSet to="/lib">
    <exclude id="org.codehaus.plexus:plexus-utils"/>
    <artifact id="org.apache.maven:maven-core:3.3.9"/>
  </artifactSet>
</runtime>
```