To build a new distribution of this application you need to run gradle installApp.

To use the embeded jre, add the following to the [installDir]/bin/[appName].bat file 
in the line before @rem Find java.exe set JAVA_HOME=jre7_x64\
If running this application from a 32 bit machine, the above referenced 64 bit embeded jre won't work, 
instead use set the DEFAULT_JVM_OPTS=-XX:+AggressiveHeap

