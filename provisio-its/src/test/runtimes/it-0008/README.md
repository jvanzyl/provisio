Validating standard resource filtering using ${variables} works. Presto relies on this for the production of their server.

``` xml
<runtime>
  <!-- Launcher -->
  <artifactSet to="bin">
    <artifact id="io.airlift:launcher:tar.gz:properties:0.142">
      <unpack filter="true" />
    </artifact>
  </artifactSet>
</runtime>
```
