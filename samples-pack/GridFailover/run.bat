@echo off
set JAVA_ARGS=-cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.example.gridfailover.ConnectionListener

call java %JAVA_ARGS%
