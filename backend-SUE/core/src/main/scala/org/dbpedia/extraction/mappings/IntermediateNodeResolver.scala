package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty, OntologyClass}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{TemplateNode, PageNode}

import scala.collection.mutable.ArrayBuffer

class IntermediateNodeResolver(nodeClass : OntologyClass,
                correspondingProperty : OntologyProperty,
                mappings : List[PropertyMapping], // must be public val for statistics
                context : {
                  def ontology : Ontology
                  def language : Language
                }) extends IntermediateNodeMapping(nodeClass, correspondingProperty, mappings, context) with PropertyResolver {

  override def resolve(node: TemplateNode, updatePropertyUri: String): Seq[WikiDML] = {

    println("Here...")
    null
  }

}
