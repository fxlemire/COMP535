call "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 14.1.4\plugins\maven\lib\maven3\bin\mvn" compile
call "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 14.1.4\plugins\maven\lib\maven3\bin\mvn" compile assembly:single
java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router1.conf
