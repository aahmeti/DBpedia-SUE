cp ~/workspaceIDEA/extraction-framework-master-2/core/target/core-4.1-SNAPSHOT-jar-with-dependencies.jar lib/
mvn install:install-file -Dfile=lib/core-4.1-SNAPSHOT-jar-with-dependencies.jar -DgroupId=org.wu.ac.at -DartifactId=extractor -Dversion=0.1 -Dpackaging=jar
