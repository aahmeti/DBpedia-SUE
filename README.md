# DBPedia-SUE

DBpedia-SUE is a prototype extending DBpedia Extraction Framework, which aims to address the problem of updating or curating Wiki pages via SPARQL/Update by resolving DBpedia mappings. 

## Documentation

The main entry file `InfoboxSandboxCustom.scala` is located under `./core/src/main/scala/org.dbpedia.extraction/`.

Its methods are used in the GUI tool, screencast available [here](https://www.dropbox.com/s/xdbxjup8dvisajj/screencast.wmv?dl=0).

### Experiments 

- Get number of occurrences for resolved alternatives of an update, computed by a sample of the same type and same property name.

	`scala TestStatisticsFromDBpedia.main(Array(""))`
	
