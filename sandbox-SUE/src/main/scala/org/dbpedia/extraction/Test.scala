package org.dbpedia.extraction

import java.io.File
import java.util.NoSuchElementException

import at.tuwien.dbai.rewriter.UtilFunctions
import com.hp.hpl.jena.sparql.modify.request.UpdateModify
import com.hp.hpl.jena.update.{UpdateRequest, UpdateFactory}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.Language

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
 * Created by aahmeti on 28/10/2016.
 */
class Test {



}


object TestGroundTriplesFromUpdate {

  def main(args: Array[String]): Unit = {

    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

    val test = new InfoboxSandboxCustom(null, "")
    val ans = test.getGroundTriplesFromUpdate(update)

    for (del <- ans._1) println(del)
    for (ins <- ans._2) println(ins)

  }

}

object TestLocalConsistencyCheck {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = new File("./data/downloads/Santi_Cazorla/709848090.xml") // no need for this, subject is taken from update
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val choiceDML = Seq(new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "name", newValue = "Santi", operation = "INSERT"),
      new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "playername", newValue = "Santi CZ", operation = "INSERT"),
      new WikiDML("http://en.wikipedia.org/wiki/Thierry_Henry", "infobox football biography", "playername", newValue = "Thierry", operation = "INSERT"))

    //new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "managerYears", newValue="Arsenal", operation="INSERT"))

    val b = test.checkConsistency(choiceDML, null)

    if (b)
      println("The update: " + choiceDML + " is consistent")
    else
      println("The update: " + choiceDML + " is not consistent")

  }
}

object TestGetMappingFromSubject {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null // no need for this, subject is taken from update
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val title = "Santi_Cazorla"

    test.getMappingType(title)

  }

}


object TestConsistencyCheck {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = new File("./data/downloads/Thierry_Henry/test") // no need for this, subject is taken from update
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

    val b = test.checkConsistency(update)

    if (b)
      println("The update: " + update + " is consistent")
    else
      println("The update: " + update + " is not consistent")

  }

}

object TestInfoboxCount {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = "_ambig.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val titles = Seq("Santi_Cazorla", "Thierry_Henry")

    val infoboxProperties =

      Seq(new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "name",
        newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "fullyname",
          newValue = "Dennis", operation = "INSERT"))

    println(test.countInfoboxProperties(titles, infoboxProperties))
  }

}

object TestInfoboxCountFootballPlayersStats {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = "_ambig.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val filePath = "/ISWC/english-players.xml"
    val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

    val infoboxProperties =

      Seq(new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "name",
        newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "fullname",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "playername",
          newValue = "Dennis", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "dateofbirth",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "birth_date",
          newValue = "Dennis", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "cityofbirth",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "countryofbirth",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "birth_place",
          newValue = "Dennis", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "dateofdeath",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "death_date",
          newValue = "Dennis", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "cityofdeath",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "countryofdeath",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "death_place",
          newValue = "Dennis", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs1",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs2",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs3",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs1",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs2",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs3",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs4",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs5",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs6",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs7",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs8",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs9",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs10",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam1",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam2",
          newValue = "Dennis", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam3",
          newValue = "Dennis", operation = "INSERT")
      )

    println(test.countInfoboxProperties(filePath, infoboxProperties, title))
  }

}

object TestInfoboxCountClubsStats {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = "_ambig.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val filePath = "/ISWC/english-teams.xml"
    val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

    val infoboxProperties =
      Seq(new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "clubname",
        newValue = "Arsenal", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "fullname",
          newValue = "Arsenal", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "shortname",
          newValue = "Arsenal", operation = "INSERT"))

    println(test.countInfoboxProperties(filePath, infoboxProperties, title))
  }

}


object TestInfoboxCountCitiesStats {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = "_ambig.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val filePath = "/ISWC/english-settlements.xml"
    val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

    val infoboxProperties =
      Seq(new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "name",
        newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "official_name",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "native_name",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin1",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin2",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin3",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin4",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin5",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin6",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin7",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin8",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin9",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin1_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin2_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin3_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin4_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin5_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin6_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin7_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin8_country",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin9_country",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title1",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title2",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title3",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title4",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name1",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name2",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name3",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name4",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date1",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date2",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date3",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "population_as_of",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "pop_est_as_of",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "population_total",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "pop_est",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "postal_code",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "postal2_code",
          newValue = "London", operation = "INSERT"),

        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name1",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name2",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name3",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name4",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name5",
          newValue = "London", operation = "INSERT"),
        new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name6",
          newValue = "London", operation = "INSERT"))


    println(test.countInfoboxProperties(filePath, infoboxProperties, title))
  }

}

object TestResourcesWithSimilarProperties {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = "_ambig.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

    val query = test.getQueryForResourcesWithSamePredicates(100, null, "") // fixme

    println(query)
  }

}

object TestInfoboxPropertyGaps {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = ".xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia04.ru"))

    val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]
    val insertQuads = updateMod.getInsertQuads

    val classType:String = "<http://dbpedia.org/ontology/Settlement>"
    //        val classType:String = "<http://dbpedia.org/ontology/SoccerPlayer>"
    //    val classType:String = "<http://dbpedia.org/ontology/Band>"
    //    val classType:String = "<http://dbpedia.org/ontology/AdministrativeRegion>"
    //    val classType:String = "<http://dbpedia.org/ontology/River>"
    //    val classType:String = "<http://dbpedia.org/ontology/SoccerClub>"
    //    val classType:String = "<http://dbpedia.org/ontology/University>"
    //    val classType:String = "<http://dbpedia.org/ontology/BasketballPlayer>"
    //      val classType:String = "<http://dbpedia.org/ontology/Actor>"
    //     val classType:String = "<http://dbpedia.org/ontology/Skier>"
    //        val classType: String = "<http://dbpedia.org/ontology/Film>"

    val queries = test.getQueryForResourcesWithSamePredicates(100, insertQuads.get(0).getPredicate.toString(), classType)
    println(queries)

    val subjects = new ArrayBuffer[String]()

    for (query <- queries) {

      val rs = test.getQueryResultsFromDBpedia(query)

      while (rs.hasNext()) {

        val sol = rs.nextSolution.get("?Y").toString
        //        println(sol)
        val title = sol.substring(sol.lastIndexOf("/") + 1)
        if (!subjects.contains(title))
          subjects += title
        // download the pages
      }
    }

    val wikiDMLs = test.resolveUpdate(update)

    val newInfoboxProperties = new ArrayBuffer[WikiDML]()

    // flatten wikiDMLs
    val setWikiDML = wikiDMLs._1.toSeq.flatten.flatten

    for (infobox <- setWikiDML) {

      if (infobox.property.startsWith("twin") && !infobox.property.equals("twins"))
        newInfoboxProperties += infobox
    }

    test.countInfoboxPropertiesWithGaps(subjects, newInfoboxProperties)

  }

}



object TestNewStatisticsFromUI {

  def main(args: Array[String]): Unit =
  {

    val subject = "<http://dbpedia.org/resource/Walt_Disney>" // change
  //val predicate = "http://dbpedia.org/ontology/birthDate" // change
  //val predicate = "http://dbpedia.org/ontology/team" // change
  val predicate = "http://xmlns.com/foaf/0.1/homepage" // change
  val sample = 100  // change

    //the update comes from the UI
    val testDataRootDir = null
    val mappingFileSuffix = ".xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)
    var updateStr = "./data/updates/changeInfoboxPerson.ru"
    //var updateStr = "./data/updates/dbpedia01.ru"
    //var updateStr = "./data/updates/changeTaxobox.ru"
    val update = UpdateFactory.create(UtilFunctions.readFile(updateStr))
    val wikiDMLs = test.resolveUpdate(update)

    // flatten wikiDMLs
    val setWikiDML = wikiDMLs._1.toSeq.flatten


    // this is the call from the UI, i.e., subject in Dbpedia, Predicate in Dbpedia and wiki alternatives
    println(test.getStatResultsAlternatives(subject,predicate,sample,setWikiDML))

  }

}

object TestStatistics {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = null
    val mappingFileSuffix = ".xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    // defaults
    var updateStr = "./data/updates/dbpedia04.ru"
    var classType = "<http://dbpedia.org/ontology/SoccerPlayer>"
    var sampleSize = 100

    if (args.length == 3)
    {
      updateStr = args(0)

      if (args(1).equals("SoccerPlayer"))
        classType = "<http://dbpedia.org/ontology/SoccerPlayer>"
      else if (args(1).equals("Film"))
        classType = "<http://dbpedia.org/ontology/Film>"
      else if (args(1).equals("University"))
        classType = "<http://dbpedia.org/ontology/University>"
      else if (args(1).equals("Settlement"))
        classType = "<http://dbpedia.org/ontology/Settlement>"

      sampleSize = args(2).toInt
    }


    val update = UpdateFactory.create(UtilFunctions.readFile(updateStr))

    val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]
    val insertQuads = updateMod.getInsertQuads

    val queries = test.getQueryForResourcesWithSamePredicates(sampleSize,
      insertQuads.get(0).getPredicate.toString(), classType)

    val subjects = new ArrayBuffer[String]()

    for (query <- queries) {

      val rs = test.getQueryResultsFromDBpedia(query)

      while (rs.hasNext()) {

        val sol = rs.nextSolution.get("?Y").toString
        //        println(sol)
        val title = sol.substring(sol.lastIndexOf("/") + 1)
        if (!subjects.contains(title))
          subjects += title
        // download the pages
      }
    }

    val wikiDMLs = test.resolveUpdate(update)

    // flatten wikiDMLs
    val setWikiDML = wikiDMLs._1.toSeq.flatten.flatten

    println(test.countInfoboxProperties(subjects, setWikiDML))

  }

}

object TestUpdate {

  def main(args: Array[String]): Unit = {

    //  Start with no infobox and general mapping (no prefix)
    val testDataRootDir = null
    val mappingFileSuffix = ".xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    //  Instantiate General update to DBpedia and Group it by Subject
    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia04.ru"))
    val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

    var atomicUpdate: UpdateRequest = null
    if (updateMod.getWherePattern.toString.contains("?")) {
      // general update contains one variable at least
      atomicUpdate = test.instantiateGeneralUpdate(update)
    }
    else
      atomicUpdate = update

    println(test.resolveUpdate(atomicUpdate))
    println("Done")

  }

}

object TestUpdateSemantics {

  def main(args: Array[String]): Unit = {

    // infobox
    val testDataRootDir = new File("./data/downloads/Santi_Cazorla/703998546.xml")

    // mappings
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    // update
    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01a.ru"))

    val rewUpdate = test.applyUpdateSemantics(update, "brave")

    println(rewUpdate)
  }

}

object TestInsertUpdate {

  def main(args: Array[String]): Unit = {

    // infobox
    val testDataRootDir = new File("./data/downloads/Santi_Cazorla/703998546.xml")

    // mappings
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    // update
    val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01a.ru"))

    val ins = test.getGroundTriplesFromUpdate(update)._2

    test.update = ins(0)

    val res = test.resolveForLanguage(testDataRootDir, Language.English)

    println(res)

  }

}


object TestDiffFromInfoboxUpdate {

  def main(args: Array[String]): Unit = {

    // infobox
    val testDataRootDir = new File("./data/downloads/Santi_Cazorla/709848090.xml")

    // mappings
    val mappingFileSuffix = "_ambig.xml"

    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    // inserts
    val choiceDML = Seq(new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "name", newValue = "Santi", operation = "INSERT"),
      new WikiDML("Santi_Cazorla", "infobox football biography", "playername", newValue = "Santi CZ", operation = "INSERT"))

    println(test.getDiffFromInfoboxUpdate(choiceDML))

  }
}

// use this for extracting a page
object TestInfoboxSandboxCustomMappings {

  def main(args: Array[String]): Unit = {

    // Downloaded Infobox-es
    val testDataRootDir = new File("./data/downloads/test/Broward_County_Library.xml")

    // Mappings suffix
    val mappingFileSuffix = "_test.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

    println(test.renderForLanguage(testDataRootDir, Language.English))

  }
}

object TestNew2 {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = new File("./data/downloads/test/Broward_County_Library.xml")

    // Mappings suffix
    val mappingFileSuffix = "_test.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

  }

}

object TestNew {

  def main(args: Array[String]): Unit = {

    val t: Test = new Test()

    val ontoFile = new File("ontology.xml");
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)

    val lines = scala.io.Source.fromFile("ontology.xml").mkString

    println(lines)
    println(lines)
    val ontoObj = new OntologyReader().read(ontologySource)

  }
}

object TestNew3 {

  def main(args: Array[String]): Unit = {

    val testDataRootDir = new File("./data/downloads/test/Broward_County_Library.xml")

    // Mappings suffix
    val mappingFileSuffix = "_test.xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

  }
}