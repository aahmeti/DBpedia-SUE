package org.dbpedia.extraction.mappings

import java.io.Serializable

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.language.reflectiveCalls

/**
 *  Extracts structured data based on hand-generated mappings of Wikipedia infoboxes to the DBpedia ontology.
 */
class MappingExtractor(
  context : {
    def mappings : Mappings
    def redirects : Redirects
  }
)
extends PageNodeExtractor
{
  private val templateMappings = context.mappings.templateMappings
  private val tableMappings = context.mappings.tableMappings

  private val resolvedMappings = context.redirects.resolveMap(templateMappings)

  override val datasets = templateMappings.values.flatMap(_.datasets).toSet ++ tableMappings.flatMap(_.datasets).toSet

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    val graph = extractNode(page, subjectUri, pageContext)

    if (graph.isEmpty) Seq.empty
    else splitInferredFromDirectTypes(graph, page, subjectUri)
  }

  /**
   * Extracts a data from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def extractNode(node : Node, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    //Try to extract data from the node itself
    val graph = node match
    {
      case templateNode : TemplateNode =>
      {
        resolvedMappings.get(templateNode.title.decoded) match
        {
          case Some(mapping) => mapping.extract(templateNode, subjectUri, pageContext)
          case None => Seq.empty
        }
      }
      case tableNode : TableNode =>
      {
        tableMappings.flatMap(_.extract(tableNode, subjectUri, pageContext))
      }
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.
    if(graph.isEmpty)
    {
      node.children.flatMap(child => extractNode(child, subjectUri, pageContext))
    }
    else
    {
      graph
    }
  }


  override def extractWithInfoboxChanges(page : PageNode, subjectUri : String, pageContext : PageContext, wikiUpdate: Seq[WikiDML]) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    val graph = extractNodeWithInfoboxChanges(page, subjectUri, pageContext, wikiUpdate)

    if (graph.isEmpty) Seq.empty
    else splitInferredFromDirectTypes(graph, page, subjectUri)
  }

  /**
   * Extracts a data from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def extractNodeWithInfoboxChanges(node : Node, subjectUri : String, pageContext : PageContext,  wikiUpdates: Seq[WikiDML]) : Seq[Quad] =
  {
    // Try to extract data from the node itself

    val graph = node match {
      case templateNode: TemplateNode => {
        resolvedMappings.get(templateNode.title.decoded) match {
          case Some(mapping) => // fixme: perhaps here check also mapping == wikiUpdate.infobox ?

              var res = new ArrayBuffer[Quad]()
              var list = templateNode.children.toBuffer

              for (wikiUpdate <- wikiUpdates) {
                //val wikiUpdate  = wikiUpdates(0)

                wikiUpdate.operation match {

                  case "DELETE" =>

                    // TODO: checkConditionalMappings
                    // if (isSetConditionalMapping(oldWikiUpdates, wikiUpdate)) {
                    val p = new PropertyNode(wikiUpdate.property, List(new TextNode(wikiUpdate.newValue, 0)), 0)

//                    val list = templateNode.children.toBuffer

                    var index = -1
                    for (i <- 0 until list.length) {
                      if (list(i).key == p.key) {
                        index = i
                        // TODO: break
                      }
                    }

                    if (index != -1)
                      list.remove(index)

//                    val newTemplateNode =
//                      new TemplateNode(templateNode.title, list.toList, templateNode.line)
//
//                    newTemplateNode.parent = node.root
//
//                    res ++= mapping.extract(newTemplateNode, subjectUri, pageContext)
                  //               }

                  case "UPDATE" =>

                    val p = new PropertyNode(wikiUpdate.property, List(new TextNode(wikiUpdate.newValue, 0)), 0)

//                    var list = templateNode.children.toBuffer

                    var index = -1
                    for (i <- 0 until list.length) {
                      if (list(i).key == wikiUpdate.property) {
                        if (list(i).children(0).toWikiText == wikiUpdate.oldValue) {
                          // TODO: Is it sufficient children(0)?
                          index = i
                        }
                      }
                    }

                    if (index != -1) {
                      list.remove(index)
                      list.+=(p)
                    }
//                    val newTemplateNode =
//                      new TemplateNode(templateNode.title, list.toList, templateNode.line)
//
//                    newTemplateNode.parent = node.root
//
//                    res ++= mapping.extract(newTemplateNode, subjectUri, pageContext)

                  case "INSERT" =>


                    val p = new PropertyNode(wikiUpdate.property, List(new TextNode(wikiUpdate.newValue, 0)), 0)

//                    val list = templateNode.children.toBuffer

                    var index = -1
                    for (i <- 0 until list.length) {
                      if (list(i).key == p.key) {
                        index = i
                        // TODO: break
                      }
                    }

                    if (index != -1)
                      list.remove(index)

                    list = list :+ p

//                    val newTemplateNode =
//                      new TemplateNode(templateNode.title, list.toList, templateNode.line)
//
//                    newTemplateNode.parent = node.root
//
//                    res ++= mapping.extract(newTemplateNode, subjectUri, pageContext)
                }
              }
              val newTemplateNode =
                new TemplateNode(templateNode.title, list.toList, templateNode.line)

              newTemplateNode.parent = node.root

              res ++= mapping.extract(newTemplateNode, subjectUri, pageContext)

              res



          case None => Seq.empty
        }
      }
      case tableNode: TableNode => {
        tableMappings.flatMap(_.extractWithInfoboxChanges(tableNode, subjectUri, pageContext, wikiUpdates))
      }
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.

    if(graph.isEmpty)
    {
      node.children.flatMap(child => extractNodeWithInfoboxChanges(child, subjectUri, pageContext, wikiUpdates))
    }
    else
    {
      graph
    }

  }

//  private def isSetConditionalMapping(oldWikiUpdates:Seq[WikiDML], wikiUpdate: WikiDML) = {
//
//    if (wikiUpdate.operation == "DELETE") {
//
//      if () {
//
//        for (wikiUpdate <- oldWikiUpdates) {
//
//
//        }
//      }
//
//
//
//    }
//
//
//
//  }


  override def resolve(node: PageNode, subjectUri: String=null, context:PageContext=null, updateSubjectUri: String=null, updatePropertyUri: String, updateValue: String, updateOperation: String): Seq[WikiDML] = {

    var res: Seq[WikiDML] = null

    if (node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty

    res = resolveNode(node, subjectUri, context, updateSubjectUri, updatePropertyUri, updateValue, updateOperation)

    res
  }

  /**
   * Resolves (checks for equivalence) a propertyURI from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def resolveNode(node: Node, subjectUri: String=null, context:PageContext=null, updateSubjectUri: String=null, updatePropertyUri: String, updateValue:String, updateOperation:String): Seq[WikiDML] = {

    val properties = node match {
      case templateNode: TemplateNode => {
        resolvedMappings.get(templateNode.title.decoded) match {
          case Some(mapping) => mapping.resolve(templateNode, subjectUri, context, updateSubjectUri,  updatePropertyUri, updateValue, updateOperation)
          case None => Seq.empty
        }
      }
      case tableNode: TableNode => {
        tableMappings.flatMap(_.resolve(tableNode, subjectUri, context, updateSubjectUri, updatePropertyUri, updateValue, updateOperation))
      }
      case _ => Seq.empty
    }

    if(properties == Seq.empty) {
      node.children.flatMap(child => resolveNode(child, subjectUri, context, updateSubjectUri, updatePropertyUri, updateValue, updateOperation))
    }
    else {
      properties
    }
  }

  private def splitInferredFromDirectTypes(originalGraph: Seq[Quad], node : Node, subjectUri : String) : Seq[Quad] = {
    node.getAnnotation(TemplateMapping.CLASS_ANNOTATION) match {
      case None => {
        originalGraph
      }
      case Some(nodeClass) => {
        val adjustedGraph: Seq[Quad]=
          for (q <- originalGraph)
            yield
              // We split the types for the main resource only by checking the node annotations
              if (q.dataset.equals(DBpediaDatasets.OntologyTypes) &&
                  q.subject.equals(subjectUri) &&
                  !q.value.equals(nodeClass.toString) )
                q.copy(dataset = DBpediaDatasets.OntologyTypesTransitive.name)
              else q

        adjustedGraph
      }
    }
  }

}
