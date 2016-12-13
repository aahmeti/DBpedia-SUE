package org.dbpedia.updateresolution

import org.scalatest._

class UpdateSpec extends FlatSpec with Matchers {

  "UpdatePattern" should "represent rdf insertions" in {
    val p = UpdatePattern.ri("foaf:name")
    p.rdfInsert.size should be (1)
    p.rdfInsert(0).predicate should be ("foaf:name")
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val p = UpdatePattern.ri("foaf:name")

    val m = Map("foaf:name"->"name")

    p.iso(m, UpdatePattern.RDF_INSERT )
    p.wikiInsert.size should be (1)
    p.wikiInsert(0).predicate should be ("name")
  }
}