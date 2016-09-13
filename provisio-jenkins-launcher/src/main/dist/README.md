# Jenkins Distribution

Jenkins distribution has the following structure:

```
jenkins
├── bin
│   ├── launcher
│   ├── launcher.properties
│   ├── launcher.py
│   └── procname
│       └── Linux-x86_64
│           └── libprocname.so
├── etc
│   ├── config.properties
│   └── jvm.config
├── lib
│   └── jenkins-war-<jenkinsVersion>.jar
├── plugins
│   ├── credentials.jpi
│   ├── credentials.jpi.pinned
│   ├── git-client.jpi
│   ├── git.jpi
│   ├── github-api.jpi
│   ├── github-pullrequest.jpi
│   ├── github.jpi
│   ├── plain-credentials.jpi
│   ├── plugins.txt
│   ├── postscriptbuild.jpi
│   ├── scm-api.jpi
│   └── token-macro.jpi
└─ jenkins-work (created upon startup)
```

Step into the `bin` directory and use the `launcher` script to run or start the application. For example, if you want to run the application in the foreground do the following:

```
cd bin
./launcher run
```

By default the place where Jenkins will do all its work is in a directory parallel to the root of the distribution called `jenkins-work`. The plugins will be loaded from the distribution's `plugins` directory but they will be copied, unpacked and loaded from the `jenkins-work/plugins` directory. So we should be able to safely unpack new versions using OneOps and not interfere with the `jenkins-work` directory where the global configuration, jobs and slave configurations will live.