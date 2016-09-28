package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.dataparser.{DateTimeParser,StringParser}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.mappings.DateIntervalMappingConfig._
import org.dbpedia.extraction.wikiparser.{PropertyNode, NodeUtil, TemplateNode}
import java.lang.IllegalStateException
import scala.language.reflectiveCalls

class DateIntervalMapping ( 
  val templateProperty : String, //TODO CreateMappingStats requires this to be public. Is there a better way?
  startDateOntologyProperty : OntologyProperty,
  endDateOntologyProperty : OntologyProperty,
  context : {
    def redirects : Redirects  // redirects required by DateTimeParser
    def language : Language 
  }
) 
extends PropertyMapping
{
  private val logger = Logger.getLogger(classOf[DateIntervalMapping].getName)

  private val startDateParser = new DateTimeParser(context, rangeType(startDateOntologyProperty))
  private val endDateParser = new DateTimeParser(context, rangeType(endDateOntologyProperty))

  private val splitPropertyNodeRegex = splitPropertyNodeMap.getOrElse(context.language.wikiCode, splitPropertyNodeMap("en"))
  private val presentStrings : Set[String] = presentMap.getOrElse(context.language.wikiCode, presentMap("en"))
  private val sinceString = sinceMap.getOrElse(context.language.wikiCode, sinceMap("en"))
  private val onwardString = onwardMap.getOrElse(context.language.wikiCode, onwardMap("en"))
  private val splitString = splitMap.getOrElse(context.language.wikiCode, splitMap("en"))

  // TODO: the parser should resolve HTML entities
  private val intervalSplitRegex = "(?iu)(—|–|-|&mdash;|&ndash;" + ( if (splitString.isEmpty) "" else "|" + splitString ) + ")"
  
  override val datasets = Set(DBpediaDatasets.OntologyProperties)

  override def extract(node : TemplateNode, subjectUri: String, pageContext : PageContext) : Seq[Quad] =
  {
    // replicate standard mechanism implemented by dataparsers
    for(propertyNode <- node.property(templateProperty))
    {
       // for now just return the first interval
       // waiting to decide what to do when there are several
       // see discussion at https://github.com/dbpedia/extraction-framework/pull/254
       // return NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).flatMap( node => extractInterval(node, subjectUri).toList )
       return NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex)
                      .map( node => extractInterval(node, subjectUri) )
                      .dropWhile(e => e.isEmpty).headOption.getOrElse(return Seq.empty)
    }
    
    Seq.empty
  }

  override def resolve(input: TemplateNode, originalSubjectUri : String, pageContext : PageContext, updateSubjectUri:String, updatePropertyUri: String, updateValue: String, updateOperation: String): Seq[WikiDML] = {

    println("Resolving DateIntervalMapping...")

    for(propertyNode <- input.property(templateProperty)) {

      val splitNodes = NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).map(node => splitIntervalNode(node))

      //Can only map exactly two values onto an interval
      if(splitNodes.size > 2 || splitNodes.size  <= 0)
      {
        return Seq.empty
      }

      if (startDateOntologyProperty.uri == updatePropertyUri) {

        val startDateText = splitNodes(0) match
        {
          case List(start, end) => start
        }
//        val startDate = startDateParser.parse(splitNodes(0)).getOrElse(return Seq.empty)

        return Seq(new WikiDML(input.parent.sourceUri.toLowerCase,
          input.title.decoded.toLowerCase,
          propertyNode.key,
          startDateText.children(0).toString,
          updateValue,
          updateOperation
        ))
      }

      if (endDateOntologyProperty.uri == updatePropertyUri) {

        val endDateText = splitNodes match
        {
          case List(start, end) => end
        }
        //        val startDate = startDateParser.parse(splitNodes(0)).getOrElse(return Seq.empty)

        return Seq(new WikiDML(input.parent.sourceUri.toLowerCase,
          input.title.decoded.toLowerCase,
          propertyNode.key,
          endDateText.toString,
          updateValue,
          updateOperation
        ))
      }

    }
    Seq.empty

  }

    def extractInterval(propertyNode : PropertyNode, subjectUri: String) : Seq[Quad] =
  {
      //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
      val splitNodes = splitIntervalNode(propertyNode)

      //Can only map exactly two values onto an interval
      if(splitNodes.size > 2 || splitNodes.size  <= 0)
      {
        return Seq.empty
      }

      //Parse start; return if no start year has been found
      val startDate = startDateParser.parse(splitNodes(0)).getOrElse(return Seq.empty)

      //Parse end
      val endDateOpt = splitNodes match
      {
        //if there were two elements found
        case List(start, end) => end.retrieveText match
        {
          //if until "present" is specified through one of the special words, don't write end triple
          case Some(text : String) if presentStrings.contains(text.trim.toLowerCase) => None

          //normal case of specified end date
          case _ => endDateParser.parse(end)
        }

        //if there was only one element found
        case List(start) => StringParser.parse(start) match
        {
          //if in a "since xxx" construct, don't write end triple
          case Some(text : String) if (text.trim.toLowerCase.startsWith(sinceString) 
                                    || text.trim.toLowerCase.endsWith(onwardString)) => None

          //make start and end the same if there is no end specified
          case _ => Some(startDate)
        }

        case _ => throw new IllegalStateException("size of split nodes must be 0 < l < 3; is " + splitNodes.size)
      }

      //Write start date quad
      val quad1 = new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, startDateOntologyProperty, startDate.toString, propertyNode.sourceUri)

      //Writing the end date is optional if "until present" is specified
      for(endDate <- endDateOpt)
      {
        //Validate interval
        if(startDate > endDate)
        {
          logger.fine("startDate > endDate")
          return Seq(quad1)
        }

        //Write end year quad
        val quad2 = new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, endDateOntologyProperty, endDate.toString, propertyNode.sourceUri)

        return Seq(quad1, quad2)
      }

      return Seq(quad1)
  }

  private def splitIntervalNode(propertyNode : PropertyNode) : List[PropertyNode] =
  {
    //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
    val splitNodes = NodeUtil.splitPropertyNode(propertyNode, intervalSplitRegex)

    //Did we split a date? e.g. 2009-10-13
    if(splitNodes.size > 2)
    {
      NodeUtil.splitPropertyNode(propertyNode, "\\s" + intervalSplitRegex + "\\s")
    }
    else
    {
      splitNodes
    }
  }
  
  private def rangeType(property: OntologyProperty) : Datatype =
  {
    if (! property.range.isInstanceOf[Datatype]) throw new IllegalArgumentException("property "+property+" has range "+property.range+", which is not a datatype")
    property.range.asInstanceOf[Datatype]    
  }

}
