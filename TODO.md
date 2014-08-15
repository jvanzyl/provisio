- synchronize the participant with the plugin
  - test the ordering in the reactor
- specify way to make the archive and attach properly
- use provisio for the presto plugins
- add action to copy files into the runtime
- make the action descriptors more pluggable
- add tests for the execution of plugins
- create a test plugin
- catch version parsing errors and report them correctly
- use mustache for filtering
- make TDM 
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