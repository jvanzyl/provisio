Validating Mustache filtering on a fileSet directory. This lets a runtime
descriptor copy selected runtime config directly instead of pre-rendering it
into `target/generated-provisio`.

``` xml
<runtime>
  <fileSet to="etc">
    <directory path="${basedir}/src/main/resources" mustache="true">
      <include>config.properties</include>
      <include>annotations.properties</include>
      <include>jvm.config</include>
    </directory>
  </fileSet>
</runtime>
```
