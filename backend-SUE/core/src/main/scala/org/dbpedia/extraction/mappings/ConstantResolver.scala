package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{TemplateNode, PageNode}

import scala.collection.mutable.ArrayBuffer

class ConstantResolver(
                        ontologyProperty: OntologyProperty,   // property
                        var value : String,     // value of the constant
                        datatype : Datatype,
                        context : {
                          def language : Language
                        }) { // extends ConstantMapping(ontologyProperty, value, datatype, context) with PropertyResolver {

   def resolve(node: TemplateNode, updatePropertyUri: String): Seq[WikiDML] = {

    println("here")
    null
  }
}
