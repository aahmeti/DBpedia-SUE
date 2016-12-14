package org.dbpedia.updateresolution

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language

import scala.collection.JavaConversions._
import org.scalatest._

import scala.collection.mutable.ArrayBuffer

class UpdateSpec extends FlatSpec with Matchers {

  "freshVar" should "increment the variable names" in {
    UpdatePattern.freshVar() should be ("?A")
    UpdatePattern.freshVar() should be ("?B")
  }

  "UpdatePattern" should "represent rdf insertions" in {
    val p = UpdatePattern.rip("foaf:name")
    p.rdfInsert.size should be (1)
    p.rdfInsert.head.predicate should be ("foaf:name")
  }

  "RdfInsert" should "translate to a WikiInsert" in {
    val p = UpdatePattern.rip("foaf:name")

    val m = Map("foaf:name"->"name")

    p.iso(m, UpdatePattern.RDF_INSERT )
    p.wikiInsert.size should be (1)
    p.wikiInsert.head.predicate should be ("name")
    p.wikiDelete.size should be (0)
  }

  "SPARQL INSERT foaf:name El Bicho" should  "become an Update" in {

    val command = """prefix : <http://dbpedia.org/resource/>
                    |prefix foaf: <http://xmlns.com/foaf/0.1/>
                    |INSERT {:Cristiano_Ronaldo foaf:name "El Bicho"} WHERE {}""".stripMargin

    val r = new RDFUpdateResolver(null,".xml")

    val result = r.updateFromUpdateQuery(command)

    println (s"Total ${result.size()} update atoms translated " )
    for( k <- result.keySet() ){
      val trans = result.get(k)
      println (s"$k : ${trans.size()} translations found")

      for( t <- trans){
        println("****************")
        println( t.toString )
      }
    }

    //val isb = new org.dbpedia.extraction.InfoboxSandboxCustom(null,".xml")
    //val resultOld = isb.updateFromUpdateQuery(command)

  }


  "TDBSerializer " should " serialize and deserialize " in {

        val test = new TDBSerialiser()

        val quads = new ArrayBuffer[Quad]()

        val homepageProperty = new OntologyProperty("foaf:homepage", Map(Language.English -> "homepage"), Map(), null, null, false, Set(), Set())
        val quad1 = new Quad(null, null, "TestPage", homepageProperty , "http://example.com", null, null)

        quads += quad1

        test.store(quads)

        test.query("SELECT * WHERE {}")
    }

}