Provide an action to alter the contents of an artifact: https://github.com/takari/provisio/issues/3

In this specific case we want to take a Jenkins WAR file and insert additional libraries into the /WEB-INF/lib directory to change the behavior of Jenkins with a init-strategy. So we'd have an action that might look like the following:

``` xml
<runtime>
  <!-- Jenkins -->
  <artifactSet to="/lib">
    <!-- We need to rename as the launcher doesn't pick up the .war extension -->
    <artifact id="org.jenkins-ci.main:jenkins-war:war:1.648" as="jenkins-war-1.648.jar">
      <alter>
        <insert>
          <artifact 
            id="com.walmartlabs.looper:looper-jenkins-init-strategy:${looperVersion}" 
            as="/WEB-INF/lib/jenkins-init-strategy:${looperVersion}"/>
        </insert>
      </alter>
    </artifact>
  </artifactSet>
</runtime>
```