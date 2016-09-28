package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{TemplateNode, PageNode}

import scala.collection.mutable.ArrayBuffer


class SimplePropertyResolver(templateProperty : String, // IntermediateNodeMapping and CreateMappingStats requires this to be public
                             ontologyProperty : OntologyProperty,
                             select : String,
                             prefix : String,
                             suffix : String,
                             transform : String,
                             unit : Datatype,
                             private var language : Language,
                             factor : Double,
                             context : {
                               def ontology : Ontology
                               def redirects : Redirects  // redirects required by DateTimeParser and UnitValueParser
                               def language : Language
                             }) extends SimplePropertyMapping(templateProperty, ontologyProperty, select, prefix, suffix,
                                transform, unit, language, factor, context) with PropertyResolver {

  override def resolve(node: TemplateNode, updatePropertyUri: String): Seq[WikiDML] = {

    println("SimplePropertyResolver is processing...")
    null
  }
}