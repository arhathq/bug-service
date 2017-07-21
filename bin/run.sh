#!/bin/sh

java -server -Xms384m -Xmx384m -Dconfig.file=application.conf -jar bugs.jar &
