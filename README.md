# DBPedia-SUE

DBpedia-SUE is a prototype extending DBpedia Extraction Framework, which aims to address the problem of updating or curating Wiki pages via SPARQL/Update by resolving DBpedia mappings. 

## Documentation

The main entry file `InfoboxSandboxCustom.scala` is located under `./core/src/main/scala/org.dbpedia.extraction/`.

Its methods are used in the GUI tool, screencast available [here](https://www.dropbox.com/s/xdbxjup8dvisajj/screencast.wmv?dl=0).

### Experiments 

- Get number of occurrences for resolved alternatives of an update, computed by a sample — retrieving entities of the given type and same property name as in update. 
	
	- `param1`: the update
	- `param2`: type (SoccerPlayer, Settlement, University, Film)
	- `param3`: size of the sample 

	* SoccerPlayer	
	`scala TestStatistics.main(Array("./data/updates/dbpedia01.ru", "SoccerPlayer", "100"))`
	*	Settlement
	`scala TestStatistics.main(Array("./data/updates/dbpedia02.ru", "Settlement", "100"))`
	* Universities
	`scala TestStatistics.main(Array("./data/updates/dbpedia03.ru", "University", "100"))`
	* Film
	`scala TestStatistics.main(Array("./data/updates/dbpedia04.ru", "Film", "100"))`	

- Get the number of fired infobox properties, given a downloaded sample of football players from Wikipedia

	`scala TestInfoboxCountFootballPlayersStats.main(Array(""))`

- Get the number of fired infobox properties, given a downloaded sample of clubs from Wikipedia

	`scala TestInfoboxCountClubsStats.main(Array(""))`

- Get the number of fired infobox properties, given a downloaded sample of cities from Wikipedia

	`scala TestInfoboxCountCitiesStats.main(Array(""))`
	
	
