Validating mustache resource filtering using {{variables}}. Moving forward Provisio will encourage people to use mustache filtering and we'll eventually make it the default.

``` xml
<runtime>
  <!-- Launcher -->
  <artifactSet to="bin">
    <artifact id="io.takari:launcher:tar.gz:properties:0.142">
      <unpack mustache="true" />
    </artifact>
  </artifactSet>
</runtime>
```
