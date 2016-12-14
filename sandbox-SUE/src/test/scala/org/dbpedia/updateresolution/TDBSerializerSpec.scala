package org.dbpedia.updateresolution

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language

import org.scalatest._
import scala.collection.mutable.ArrayBuffer

class TDBSerializerSpec extends FlatSpec with Matchers {


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
