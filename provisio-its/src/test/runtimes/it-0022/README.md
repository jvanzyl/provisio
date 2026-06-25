Validating conditional Mustache filtering: a `jvm.config` template carries a
`{{#datadog}}...{{/datadog}}` section that is rendered only when `datadog` is
true. This is the pattern for toggling optional agent properties on at
provisioning time.

``` xml
<runtime>
  <artifactSet to="etc">
    <artifact id="io.takari.provisio.its:it-0022:0.0.1-SNAPSHOT">
      <unpack mustache="true" includes="jvm.config" />
    </artifact>
  </artifactSet>
</runtime>
```
