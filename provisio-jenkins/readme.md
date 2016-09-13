# Jenkins distribution provisioning

### Usage:
`java -jar provisio-jenkins-<version>-uber.jar [-r <remoteRepo>] [-l <localRepo>] [-t <templateDir>] <config.properties> <targetDir>`

```
-r <remoteRepo>   - url of a remote repository to download maven artifacts from. Central and jenkins-ci.org repos are always added, 
                    so this option can be omitted if nothing specifc is needed to be downloaded from elsewhere.
-l <localRepo>    - defaults to <user.home>/.m2/repository, if omitted
-t <templateDir>  - additional directory with configuration templates, any files within that dir will be 
                    processed by moustache engine with config.properties as context

<config.properties> - path to .properties file with configuration, described below
<targetDir>         - directory where jenkins-installation and jenkins-work dirs will be created
```

The command above will provision the jenkins runtime and a set of initial configuration based on `<config.properties>` file.

### Jenkins http port
```
jenkins.port=<port>
```
Specifies port on which jetty server will be listening

### Jenkins version
```
jenkins.version=<version>
```

### Additional lines that will be passed to java runtime
```
system.x=-Dfoo=bar
system.y=...
```

## Plugins
Specified plugins will be installed as top-level and pinned, any transitive dependencies will also be installed using the 'minimum required' version strategy.
E.g.: if installing A and B such that A depends on C1.0 and B on C2.0, C2.0 will be installed in addition.

### Standard plugin from 'org.jenkins-ci.plugins' group
```
jenkins.plugins.<artifactId>=<version>
```

### Plugin with custom coordinates
```
jenkins.plugins.x=<groupId>:<artifactId>:<version>
```

### Including optional transitive plugin dependencies
```
jenkins.plugins.x.includeOptional=true
```

# Configuration provisioning
There are two ways to provision configuration:
1. Using simple templates (with `-t` option)
2. Using configuration mixins

## Templates
Template files are processed by [mustache](https://github.com/spullara/mustache.java) engine.
`<config.properties>` map will be passed as a context.

See also [Template merging](#merging)

## Mixins
Mixin is a java class that implements `io.provis.jenkins.config.ConfigurationMixin` which processes `<config.properties>` data and produces templates and an additional set of beans to be used in those templates.
Each mixin has a `#getId()` method that returns a unique identifier which will be used to trigger that mixin if corresponding configuration section is available in `<config.properties>`.
For example, if there is a mixin with id `github`, then this section will trigger it:
```
github.webUrl=https://github.com
github.apiUrl=https://api.github.com
github.username=bla
...
```

`provisio-jenkins` comes with a number of mixins that provide a way to configure standard features of jenkins:

### Users
Adds configuration for a number of users
```
users.<username>.email=...
users.<username>.apiToken=...
```

### Credentials
Adds credential configuration.
Currently, only StringCredential and UsernamePasswordCredential supported
```
# secret string credential
credentials.cred1.domain=...
credentials.cred1.domainDescription=...
credentials.cred1.description=...
credentials.cred1.secret=...

# username/password credential
credentials.cred2.domain=...
credentials.cred2.domainDescription=...
credentials.cred2.description=...
credentials.cred2.username=...
credentials.cred2.password=...
```

### Github
Configures [github plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin)
```
github.webUrl=...
github.apiUrl=...

# these three will be used to add a github credential to credentials plugin
github.oauthTokenId=<credentialId>
github.oauthToken=<token>
github.username=<user>

github.manageHooks=true|false (default)
```

### Github authentication
Configures [github oauth authentication](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+OAuth+Plugin)
```
security.gh.webUrl=...
security.gh.apiUrl=...
security.gh.clientId=...
security.gh.clientSecret=...
security.gh.oauthScopes=...
```

### Active directory authentication
Configures AD authentication
```
security.ad.domain=...
security.ad.server=localhost,127.0.0.1
security.ad.site=...
security.ad.bindDN=...
security.ad.bindPassword=...
security.ad.lookup=AUTO|RECURSIVE|CHAIN
# cache config should be omitted in most cases
# values below should be selected from the list of available to be consistent on ui
security.ad.cache.size=10|20|50|100|200|256 (default)|500|100
security.ad.cache.ttl=30|60|300|600 (default)|900|1800|3600
```

### JGit
Enables jgit as a git tool
```
jgit=true
```

## Custom mixins
If a mixin you want to use doesn't come from `provisio-jenkins`, but a different artifact, you can add such artifacts using:

```
config.dependencies = <groupId>:<artifactId>:<version>[, ...]
```
or 
```
config.dependencies.foo = <groupId>:<artifactId>:<version>
config.dependencies.bar = ...
```

Mixins are registered using META-INF/services approach.

## <a name="merging"></a>Template merging
Sometimes you only need to provide a section for an existing configuration file without overriding it completely, as is mostly the case with global `config.xml` jenkins configuration.

This can be done by naming your template as `<filename>-merge.xml`. For example, for `config.xml` it would be `config-merge.xml`.

Structure of such template should be the same as if it was a full config file, but would only contain a single section. If the base template into which to merge does not exist, then this `merge` template will be copied as is (but with `-merge` suffix stripped). If, however, a `<filename>.xml` already exists, then the template will be merged into it.

There are three ways a particular node can be added to an existing document.
1. Merge - contents of this node are merged into a first node of the same name in the target document.
2. Replace - this node replaces a first node of the same name in the target document.
3. Append - this node is appended at the end of the parent element in the target document.


**By default, root elements are always merged, all other elements are appended**

Source document
```
<test>
  <foo><baz/></foo>
</test>
```

Target document
```
<test>
  <foo><bar/></foo>
</test>
```

Result
```
<test>
  <foo><bar/></foo>
  <foo><baz/></foo>
</test>
```

**If you need to merge one element into another, use `merge="true"`**

Source document
```
<test>
  <foo merge="true"><baz/></foo>
</test>
```

Target document
```
<test>
  <foo><bar/></foo>
</test>
```

Result
```
<test>
  <foo>
    <bar/>
    <baz/>
  </foo>
</test>
```

**Replacing a section using `merge="replace"`**

Source document
```
<test>
  <foo merge="true"><baz/></foo>
</test>
```

Target document
```
<test>
  <foo><bar/></foo>
</test>
```

Result
```
<test>
  <foo>
    <bar/>
    <baz/>
  </foo>
</test>
```

**Adding sections inline**

Sometimes you might want to put only a snippet into a more deep template structure, but you don't want to replicate all that structure in your template.

Attributes `appendPath`, `mergePath` and `replacePath` would help with that.

**`appendPath="<path>"`**

Source
```
<foo appendPath="a/test">
  <baz/>
</foo>
```

Target
```
<a>
  <test>
    <foo><bar/></foo>
  </test>
</a>
```

Result
```
<a>
  <test>
    <foo><bar/></foo>
    <foo><baz/></foo>
  </test>
</a>
```

**`mergePath="<path>"`**

Source
```
<foo mergePath="a/test/foo" attr="bar">
  <baz/>
</foo>
```

Target
```
<a>
  <test>
    <foo><bar/></foo>
  </test>
</a>
```

Result
```
<a>
  <test>
    <foo>
      <bar/>
      <baz/>
    </foo>
  </test>
</a>
```

**`replacePath="<path>"`**

Source
```
<foo replacePath="a/test/foo" attr="bar">
  <baz/>
</foo>
```

Target
```
<a>
  <test>
    <foo><bar/></foo>
  </test>
</a>
```

Result
```
<a>
  <test>
    <foo attr="bar"><baz/></foo>
  </test>
</a>
```

**`replacePath` works even if target node does not exist**

Source
```
<foo replacePath="a/test/foo" attr="bar">
  <baz/>
</foo>
```

Target
```
<a>
  <test/>
</a>
```

Result
```
<a>
  <test>
    <foo attr="bar"><baz/></foo>
  </test>
</a>
```
