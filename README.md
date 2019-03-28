# HDS-notary-server

## Generate the thing for CC
mvn install:install-file -DgroupId=pteidlibj -DartifactId=pteidlibj -Dversion=1.0 -Dfile=pteidlibj.jar -Dpackaging=jar -DgeneratePom=true

https://stackoverflow.com/questions/1452790/will-database-file-of-sqlite3-be-damaged-when-suddenly-power-off-or-os-crash
https://www.sqlite.org/pragma.html#pragma_journal_mode
