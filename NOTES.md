There some testing capabilities for generating repositories that i need to integrate into general maven provisioning testing.

- create a declarative test mechanism where you need
  1) a descriptor
  2) a text file describing the exact shape of the runtime
  3) a validator that makes sure the expected shape matches the actual shape

- create a test for the maven distribution
- create a test for the presto distribution

- a way to test the provisioner with a real resolution setup and a repository and verify the output
- how to distinguish between a project producing a single output which is the primary artifact vs producing many secondary
- allow for non-transitive resolution: <artifact id="g:a:v" transitive="false"/>
- allow for exclusions at the artifact level, and artifact set level
- make the action descriptors more pluggable
- add tests for the execution of plugins
- create a test plugin
- catch version parsing errors and report them correctly
- use mustache for filtering

- add ${basedir} to support referencing resources correctly (done)
- specify way to make the archive and attach properly for a single archive (done)
- add action to copy files into the runtime (done)
- we have the case for a presto plugin where it produces a JAR that needs to be included in a final zip that is built. we want to refer to the JAR that is being built with no version so that it will be included but what we are producing is a zip file (done)
- allow consulting the dependency management section (done)
- support ${project.groupId|artifactId|version} (done)
- easy way to include the project you are building (done)
- find the provisioning files automatically without configuration (done)
- create a packging for provisio (done)

- how to structure provisioning action, what to use for populating configuration (done)
  - here i think that using xstream and passing it information to instantiate and configure new actions would be cool
- separate model from runtime model (done)

## Maven

- need to create a binding for the repository system session in Maven to make this work ideally
- make settings builder reusable

## Core

- operating on the graph of artifacts

## Parser

- provisio parser should only parse the model, i need to remove the logic in the parser
  - this will allow the model to be in different formats like groovy or ruby 

## Tesla Provisio Plugin
- users of the plugin should only have to specify a packaging and everything else should be taken care of by convention
- process a directory of descriptors by convention
- process all the descriptors for dependencies in the reactor
- figure out how to inject the participant in the mojo so that the descriptors are only parsed once