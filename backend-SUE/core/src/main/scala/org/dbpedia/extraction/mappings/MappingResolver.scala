package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.destinations.{Quad, Dataset}
import org.dbpedia.extraction.util.ExtractorUtils
import org.dbpedia.extraction.wikiparser._
import scala.collection.mutable.ArrayBuffer

class MappingResolver(context: {
                      def mappings : Mappings
                      def redirects : Redirects
                      }) extends MappingExtractor(context) {

  val templateMappings = context.mappings.templateMappings
  val tableMappings = context.mappings.tableMappings
  val resolvedMappings = context.redirects.resolveMap(templateMappings)

  override def resolve(input: PageNode, subjectUri:String, pageContext: PageContext, updateSubjectUri:String,  updatePropertyUri: String, updateValue: String, updateOperation: String): Seq[WikiDML] = {

    var res: Seq[WikiDML] = null
//
//    if (input.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(input.title)) return Seq.empty
//
//    //res = resolveNode(input, updatePropertyUri, updateValue, updateOperation)
//
    res
  }

  /**
   * Resolves (checks for equivalence) a propertyURI from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def resolveNode(node: Node, updatePropertyUri: String, updateValue: String, updateOperation: String): Seq[WikiDML] = {
      val properties: Seq[WikiDML] = null
//    val properties = node match {
//      case templateNode: TemplateNode => {
//        resolvedMappings.get(templateNode.title.decoded) match {
//          case Some(mapping) => //resolver .asInstanceOf[Resolver[TemplateNode]].resolve(templateNode, updatePropertyUri)
//                  mapping.resolve(templateNode, updatePropertyUri, updateValue, updateOperation)
//          case None => null
//        }
//      }
//      case tableNode: TableNode => {
////        tableMappings.flatMap(_.resolve(tableNode, updatePropertyUri, updateValue, updateOperation))
//      }
//      case _ => null
//    }
//
//    if(properties == null) {
//      node.children.flatMap(child => resolveNode(child, updatePropertyUri, updateValue, updateOperation))
//    }
//    else {
//      properties
//    }

    properties
  }
}
