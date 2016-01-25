#!/bin/sh

java -DJENKINS_HOME=../work -Dhudson.DNSMultiCast.disabled=true -jar jenkins*.war --httpPort=@PORT@
