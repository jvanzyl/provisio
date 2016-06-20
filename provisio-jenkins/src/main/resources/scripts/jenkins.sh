#!/bin/sh

java -DJENKINS_HOME=../work -Djenkins.install.runSetupWizard=false -Dhudson.DNSMultiCast.disabled=true -jar jenkins*.war --httpPort=@PORT@
