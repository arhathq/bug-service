set JAVA_OPTS=-server -Xms384m -Xmx384m
java %JAVA_OPTS% -Dconfig.file=application.conf -jar bugs.jar