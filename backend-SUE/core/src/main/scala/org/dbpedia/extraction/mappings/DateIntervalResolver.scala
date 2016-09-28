package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.wikiparser.{TemplateNode, PageNode}

import scala.collection.mutable.ArrayBuffer

class DateIntervalResolver (templateProperty : String,
                            startDateOntologyProperty : OntologyProperty,
                            endDateOntologyProperty : OntologyProperty,
                            context : {
                              def redirects : Redirects  // redirects required by DateTimeParser
                              def language : Language
                            }) extends DateIntervalMapping(templateProperty,
                              startDateOntologyProperty, endDateOntologyProperty, context) with PropertyResolver {

  override def resolve(node: TemplateNode, updatePropertyUri: String): Seq[WikiDML] = {

    val resolvedProperties:Seq[WikiDML] = null

    for(propertyNode <- node.property(templateProperty))
    {
        println(propertyNode.key)
    }

    resolvedProperties

  }
}
