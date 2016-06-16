package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.dataparser.StringParser

class ConditionalMapping( 
  val cases : List[ConditionMapping], // must be public val for statistics
  val defaultMappings : List[PropertyMapping] // must be public val for statistics
)
extends Extractor[TemplateNode]
{
  override val datasets = cases.flatMap(_.datasets).toSet ++ defaultMappings.flatMap(_.datasets).toSet

  override def extract(node: TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    for(condition <- cases)
    {
      if (condition.matches(node)) {
        val graph = condition.extract(node, subjectUri, pageContext)
        // template mapping sets instance URI
        val instanceURI = node.getAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing instance URI"))
        return graph ++ defaultMappings.flatMap(_.extract(node, instanceURI, pageContext))
      }
    }
    Seq.empty
  }

  override def extractWithInfoboxChanges(node: TemplateNode, subjectUri : String, pageContext : PageContext, wikiUpdate: Seq[WikiDML]) : Seq[Quad] =
  {
    for(condition <- cases)
    {
      println("Condition:" + condition)

      if (condition.matches(node)) {
        val graph = condition.extractWithInfoboxChanges(node, subjectUri, pageContext, wikiUpdate)
        // template mapping sets instance URI
        val instanceURI = node.getAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing instance URI"))
        return graph ++ defaultMappings.flatMap(_.extractWithInfoboxChanges(node, instanceURI, pageContext, wikiUpdate))
      }
    }
    Seq.empty
  }

  override def resolve(input: TemplateNode, originalSubjectUri : String, pageContext : PageContext, updateSubjectUri:String, updatePropertyUri: String, updateValue: String, updateOperation: String): Seq[WikiDML] = {

    println("Resolving Conditional Mapping...")

    for (condition <- cases) {

      if (condition.matches(input)) {

        val wikiDML = condition.resolve(input, originalSubjectUri, pageContext, updateSubjectUri, updatePropertyUri, updateValue, updateOperation)

        return wikiDML ++ defaultMappings.flatMap(_.resolve(input, originalSubjectUri, pageContext, updateSubjectUri, updatePropertyUri, updateValue, updateOperation))
      }
    }
    Seq.empty
  }
}
