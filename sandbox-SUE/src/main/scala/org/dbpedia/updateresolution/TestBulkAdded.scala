package org.dbpedia.updateresolution

import java.util.NoSuchElementException

import scala.io.Source


object TestBulkAdded {

  def main(args: Array[String]): Unit = {

    //  Start with no infobox and general mapping (no prefix)
    val testDataRootDir = null
    val mappingFileSuffix = ".xml"
    val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)


    val filewithInserts = "./data/updates/added.nt"

    var countExistingTriples=0
    var countMissingTriples=0
    var countresolvedWikiDMLupdates=0
    var countresolvedUNIQWikiDMLupdates=0
    var countresolvedNOTUNIQWikiDMLupdates=0
    var countresolvedalternativeAccommodationsWikiDMLupdates=0
    var countNOTresolvedWikiDMLupdate=0
    var countexceptions=0
    var countTypeInsert=0;
    var countTypeMissing=0;
    var countTypeExistingTriples=0
    var countTypeInsertResolved=0;
    var countTypeInsertNOTResolved=0;
    var total=0
    for(line <- Source.fromFile(filewithInserts).getLines()){
      total+=1
      println("\n"+total+". ======================================")
      //1.- CHECK THAT THE TRIPLE IS INDEED IN DBPEDIA (OTHERWISE IT MIGHT CAME FROM OTHER SOURCES OR BEING A TRANSITION)

      var exists = test.tripleExists(line)
      // UNCOMMENT THE FOLLOWING TO RELAX THE CONDITION AND JUST CHECK THAT THE SUBJECT AND PREDICATE ARE PRESENT
      //var exists = test.subjectandPredicateExists(line)
      //2. COUNT IF THE TRIPLE IS INSERTING A TYPE (IT'S MORE DIFFICULT)
      var isType = false
      if (line.contains("22-rdf-syntax-ns#type")){
        countTypeInsert+=1
        isType=true
      }

      if (!exists){
        countMissingTriples+=1
        println("---- Missing Triple in Dbpedia: "+line)
        if (isType)
          countTypeMissing+=1
      }
      else{
        countExistingTriples+=1
        if (isType)
          countTypeExistingTriples+=1
        val atomicUpdate = "INSERT{ "+line+"} WHERE{}";
        println("++++++ Resolving Insert: "+line)
        //println("resolveupdate!")
        try{
          var result = test.updateFromUpdateQuery(atomicUpdate)

          println("count: "+result._1(0).size)
          if (result._1(0).size>0) {
            countresolvedWikiDMLupdates += 1
            if (isType)
              countTypeInsertResolved+=1
            if (result._1(0).size==1){
              countresolvedUNIQWikiDMLupdates+=1
            }
            else{
              countresolvedNOTUNIQWikiDMLupdates+=1
              countresolvedalternativeAccommodationsWikiDMLupdates+=result._1(0).size
            }
          }
          else {
            countNOTresolvedWikiDMLupdate += 1
            if (isType)
              countTypeInsertNOTResolved+=1
          }
          println(result)
        }
        catch {
          case _: NoSuchElementException => {
            println("__NoSuchElementException")
            countNOTresolvedWikiDMLupdate+=1
            countexceptions+=1
          }
        }

      }
      println("======================================")

    }

    println("\n\n FINAL STATS ======================================")
    println("- total Triples: "+total)
    println("  - triples NOT in Dbpedia: "+countMissingTriples)
    println("  - triples in Dbpedia (processed): "+countExistingTriples)
    println("      - insertion resolved: "+countresolvedWikiDMLupdates)
    println("           - UNIQ accomodations: "+countresolvedUNIQWikiDMLupdates)
    println("           - NOT UNIQ accomodations: "+countresolvedNOTUNIQWikiDMLupdates)
    if (countresolvedNOTUNIQWikiDMLupdates!=0)
      println("               - average number of alternative accomodations: "+countresolvedalternativeAccommodationsWikiDMLupdates/countresolvedNOTUNIQWikiDMLupdates)
    println("      - insertion NOT resolved: "+countNOTresolvedWikiDMLupdate)
    println("           - Exceptions: "+countexceptions)
    println("----------------------")
    println("  - TYPE Triples: "+countTypeInsert)
    println("      - TYPE triples NOT in Dbpedia: "+countTypeMissing)
    println("      - TYPE triples in Dbpedia (processed): "+countTypeExistingTriples)
    println("           - TYPE triples resolved: "+countTypeInsertResolved)
    println("           - TYPE triples NOT resolved: "+countTypeInsertNOTResolved)


  }

}
