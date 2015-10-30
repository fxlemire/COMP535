@ECHO OFF

set number=%1

FOR %%F IN (%*) DO (
    shift

    IF %%F==-c (
        CALL "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 14.1.5\plugins\maven\lib\maven3\bin\mvn" compile
        CALL "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 14.1.5\plugins\maven\lib\maven3\bin\mvn" compile assembly:single
    )

    IF %%F==-d set debug= -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000%number%
)

SET router=conf/router%number%.conf

java%debug% -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main %router%