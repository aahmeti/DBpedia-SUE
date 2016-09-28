package org.dbpedia.extraction {

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.sources.{FileSource,XMLSource}
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import io.Source
import sys.process._

import java.io.{FilenameFilter, File}
import java.lang.IllegalStateException
import java.net.URL
import org.wayback.diff_match_patch //  _root_.

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls
import scala.io.Source
import scala.language.postfixOps
import scalax.file.Path
import scala.xml.XML
import scala.xml
import scala.math._
import scala.collection.JavaConversions._

object RevisionExtractMain {

	def main(args:Array[String]) {
		
		val title = args(0)
		//val folder = new File(args(1))
		//val ontologyFile =  args(2)
		val revision_time = args(1)
		val revision_limit = args(2).toInt
		val content = args(3).toInt
		
		val extractor = new RevisionExtractWrapper
		val result = extractor.getRevisions(title, revision_time, "trig", false, true, false) //

		println(result)
		
    }
	
}

object SingleExtractByDateWrapper {

	
	def main(args:Array[String]) {
		
		val title = args(0)
		val revision_time = args(1)
		
		val extractor = new RevisionExtractWrapper
		val result = extractor.getResourceByDate(title, revision_time, "trig", true, false) 
		
		println(result)
  }
	
}

object SingleExtractByIdWrapper {

	
	def main(args:Array[String]) {
		
		val title = args(0)
		val revision_id = args(1)
		
		val extractor = new RevisionExtractWrapper
		val result = extractor.getResourceById(title, revision_id, "trig", true, false) 
		
		println(result)
    }
	
	
}

object SingleExtractDifference {

	
	def main(args:Array[String]) {
		
		val title = args(0)
		val revision_id = args(1)
		val revision_id2 = args(2)
		
		val extractor = new RevisionExtractWrapper
		val result = extractor.getDifferencesByIds(title, revision_id, revision_id2) 
		
		println(result)
    }
	
}

class RevisionExtractWrapper {
	
	private var download_directory = "./data/downloads" // final
	private var ontology_name = "./ontology.xml"
	private var revision_limit = 100
	val WikipediaAPI_Base = "https://en.wikipedia.org/w/api.php?" // Important to have https:// otherwise sometimes fails

	val PrefixBase = "@prefix :     <http://data.wu.ac.at/wayback/dbpedia/> . \n"
	val PrefixWB = "@prefix wb:   <http://data.wu.ac.at/wayback/dbpedia/ns#> . "
	val PrefixXSD = "@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> . "
	val PrefixOWL = "@prefix owl:  <http://www.w3.org/2002/07/owl#> . "
	val PrefixRDFS = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . "
	
	// val RdfRevisionStart = ":%s/revision/id/%s a  wb:Revision ;"
	val RdfRevisionStart = "<http://data.wu.ac.at/wayback/dbpedia/%s/revision/id/%s> a  wb:Revision ;"
	val RdfRevisionEnd = "."
	
	val PrefixRevFile = "Rev_"
	//private var prefixResFile = "Res_"
	
	val ContentRDFFormat_Trig = "trig"
	val ContentRDFFormat_Trix = "trix"
	val ContentRDFFormat_Nquads = "nquads"
	
	def setConfiguration(downloadDirectory : String, ontologyName : String) {
		
		download_directory = downloadDirectory
		ontology_name = ontologyName
		
	}
	
	def isQuads(format : String) : Boolean = {
		
		var quads = false
		if(format == ContentRDFFormat_Trig || format == ContentRDFFormat_Trix || format == ContentRDFFormat_Nquads) {
			quads = true
		}
		
		return quads
		
	}
	
	def getRevisions(title : String, revisionTime : String, format : String, extractData : Boolean, clean : Boolean, originalURI : Boolean) : String = {
		
		val ontologyFile =  ontology_name
		val revision_time = revisionTime
		// val revision_limit = revisionLimit
		//val content = extractTiples
		var quads = isQuads(format)
		
		println("Start revisions extraction for: " + title)

		val folder = new File(download_directory)
	
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)
		
		val extractor = new ExtractLogic
		
		val dmp = new diff_match_patch
		
		// Empty Directory 
		//extractor.emptyDirectory(pathName)
		
		// Wikipedia API URL to get revisions
		val url = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids%7Cflags%7Ctimestamp%7Cuser%7Cuserid%7Csize%7Csha1%7Ccontentmodel%7Ccomment%7Cflagged&rvlimit=" + revision_limit + "&rvstart=" + revision_time + "&titles="  + title

		println("URL: " +url)
		
		val newFile: Path = Path.fromString(pathName + "/" + "revisions.xml")
		newFile.deleteIfExists()
		val newFile2: Path = Path.fromString(pathName + "/" + "revisions.ttl")
		newFile2.deleteIfExists()
		
		//val temp = "wget --no-check-certificate -O \"%s\" \"%s\" ".format(pathName + "/" + "revisions.xml", url)
		//println("Wget: " +temp)
		//val urlResult = temp !!
		
		// Extract revision ID from file
		//val urlResult = Source.fromURL(url).mkString
		//println("Output: " +urlResult)
		
		//val xml = XML.loadString(urlResult)
		// Crazy way of Scala to to wget from an URL write result directly to file
		val outputFile = new File(pathName + "/" + "revisions.xml")
		new URL(url) #> outputFile !!
		val xml = XML.loadFile(outputFile)
		
		//println(xml)
		//val revNodes2 = (xml \\ "revisions")
		
		val outputFileName = pathName + "/" + "revisions.ttl"
		
		var saveWikiContent:String = ""
		var saveTripleContent:String = ""

		// Write namespaces		
		//extractor.printToFile2(outputFileName, PrefixBase)
		extractor.printToFile2(outputFileName, PrefixWB)
		extractor.printToFile2(outputFileName, PrefixXSD)
		extractor.printToFile2(outputFileName, PrefixOWL)
		extractor.printToFile2(outputFileName, PrefixRDFS)
		
		var saveResult:String =  PrefixWB + "\n" + PrefixXSD + "\n" + PrefixOWL + "\n" + PrefixRDFS // PrefixBase +
		
		val revNodes1 = (xml \\ "query" \ "pages" \ "page" \ "revisions" \ "rev")
		
		// Loop through each xml node
		revNodes1.reverse.foreach{ n => 
			
		  val revID = n \ "@revid"
		  val parentid = n \ "@parentid"		  
		  
		   println("Rev: " + revID)

		   // Start a RDF node
		  
		  // Node id :revision/Vienna/607920704 
		  var resultNode:String = RdfRevisionStart.format(title, revID)
		  
		  var nodeContent:String = createRevisionNode(title, revID.mkString, n) // revID
		  resultNode = resultNode + nodeContent
		  
		  // Also add content
		  if(extractData == true) {
			  
			  		// Wikipedia Web API (check for details)
			  //val url = "http://en.wikipedia.org/w/api.php?action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=" + 1 + "&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstartid=" + revID.toString + "&export=&exportnowrap=&titles="  + title
		
			  val url = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=" + 1 + "&rvcontentformat=text%2Fx-wiki&rvstartid=" + revID.toString + "&rvdir=older&exportnowrap=&titles="  + title
			  
			  // Download single revision
			  val resultTuple = extractor.downloadAndCreateTemplate(pathName, title, url) // :Tuple2
			  val currentWikiContent = resultTuple._2
			  
			  // Determine the difference to the last version
			  var distWiki:Int = 0
			  if (saveWikiContent.length > 0) {
			     //levDist = extractor.distanceLevenshtein(currentContent, parentContent)
				 //val overlapWiki = dmp.diff_main(saveWikiContent, currentWikiContent)
				 val overlapWiki = dmp.patch_make(saveWikiContent, currentWikiContent)
				 // println(overlap)
				 distWiki = overlapWiki.size 
			  }
			  
			  // This call is just for calculating the diff
			  var currentTripleContent =  extractor.extractTriples(pathName, ontologyFile, revID.toString, title, false)
			  // this call is the real one, depending on input (quad/triple)
			  if(quads==false) {
			     extractor.extractTriples(pathName, ontologyFile, revID.toString, title, originalURI)
			  }
			  else {
			  	 extractor.extractQuads(pathName, ontologyFile, revID.toString, title, true)
			  }
			  
			  
			  var distTriple:Int = 0
			  if (saveTripleContent.length > 0) {
			     //levDist = extractor.distanceLevenshtein(currentContent, parentContent)
				 //val overlapTriple = dmp.diff_main(saveTripleContent, currentTripleContent)
				 val overlapTriple = dmp.patch_make(saveTripleContent, currentTripleContent)
				  
				 distTriple = overlapTriple.size 
			  }
			  
			  resultNode = resultNode + "; wb:diffwiki %s ;\n" .format(distWiki.toString)
			  saveWikiContent =  currentWikiContent
			  
			  resultNode = resultNode + " wb:diffrdf %s \n" .format(distTriple.toString)
			  saveTripleContent =  currentTripleContent
			  
			  
		  }
		  
		  // End blank node
		  resultNode = resultNode + RdfRevisionEnd
		  
		  // Write node
		  extractor.printToFile2(outputFileName, resultNode)
		
		  // For API
		  saveResult = saveResult + resultNode + "\n"
		  
		}
		
		return saveResult
		
	}


	def getDifferencesByDates(title : String, revision_time1 : String, revision_time2 : String) : String = {

		
		val folder = new File(download_directory)
		val ontologyFile =  ontology_name
		//val revision_time = revisionTime
		
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)

		val extractor = new ExtractLogic
		
		// Wikipedia Web API (check for details)
		val url1 = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=" + revision_time1 + "&rvdir=older&exportnowrap=&titles="  + title 
		val url2 = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=" + revision_time2 + "&rvdir=older&exportnowrap=&titles="  + title 
		
    	// Download both revisions
		val resultTuple1= extractor.downloadAndCreateTemplate(pathName, title, url1) 
		val currentWikiId1 = resultTuple1._1
			  
		val resultTuple2 = extractor.downloadAndCreateTemplate(pathName, title, url2) 
		val currentWikiId2 = resultTuple2._1
		
		println("Diff of " + currentWikiId1 + " and " + currentWikiId2)
		
		return getDifferencesByIds(title, currentWikiId1, currentWikiId2)
		
		
	}
	
	def getDifferencesByIds(title : String, revisionId1 : String, revisionId2 : String) : String = {

		val ontologyFile =  ontology_name
		
		val folder = new File(download_directory)
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)
		
		val extractor = new ExtractLogic
		val dmp = new diff_match_patch
		
		var resultNode:String =  PrefixWB + "\n" + PrefixXSD  // PrefixBase +
		
		resultNode = resultNode + RdfRevisionStart.format(title, revisionId1) + "\n"
				  
		resultNode = resultNode + " wb:revid %s ;\n" .format(revisionId1)
		resultNode = resultNode + " wb:parentid %s ;\n" .format(revisionId2)
		
		val url1 = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=" + 1 + "&rvcontentformat=text%2Fx-wiki&rvstartid=" + revisionId1 + "&rvdir=older&exportnowrap=&titles="  + title
		val resultTuple1 = extractor.downloadAndCreateTemplate(pathName, title, url1) // :Tuple2
		val currentWiki1 = resultTuple1._2

		val url2 = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=" + 1 + "&rvcontentformat=text%2Fx-wiki&rvstartid=" + revisionId2 + "&rvdir=older&exportnowrap=&titles="  + title
		val resultTuple2 = extractor.downloadAndCreateTemplate(pathName, title, url2) // :Tuple2
		val currentWiki2 = resultTuple2._2
		
		
	    // Determine the difference to the  versions
		//val overlapWiki = dmp.diff_main(currentWiki1, currentWiki2)
		val overlapWiki = dmp.patch_make(currentWiki1, currentWiki2)
		val distWiki = overlapWiki.size 

		val currentTriple1 = extractor.extractTriples(pathName, ontologyFile, revisionId1, title, false)
		
		// Not to good solution, splitting the result an put it to a list to sort
		val currentTriple2 = extractor.extractTriples(pathName, ontologyFile, revisionId2, title, false)

		 // Determine the difference to the  versions (patch (severals diffs) is better than diff)
		//val overlapTriple= dmp.diff_main(currentTriple1, currentTriple2)
		val overlapTriple= dmp.patch_make(currentTriple1, currentTriple2)
		val distTriple = overlapTriple.size 
		
		//println("Overlap:")
		//println(overlapTriple)
		
		//val scalaSet = overlapTriple.toSet
		//for( diff_element <- scalaSet){
		//	println(diff_element)
		//}
		
		resultNode = resultNode + " wb:diffwiki %s ;\n".format(distWiki.toString) // 
		resultNode = resultNode + " wb:diffrdf %s . ".format(distTriple.toString) // 
			  
		
		return resultNode
		
		
	}
	
	def getRevisionById(title : String, revisionId : String) : String = {
	
		val folder = new File(download_directory)
		val ontologyFile =  ontology_name
		
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)
		

		// Wikipedia API URL to get a single revisions
		val url = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|flags|timestamp|user|userid|size|sha1|contentmodel|comment|flagged&rvstartid=" + revisionId + "&rvendid=" + revisionId + "&titles="  + title

		val outputFile = new File(pathName + "/ " + PrefixRevFile + revisionId + ".xml")
		
		new URL(url) #> outputFile !!
		val xml = XML.loadFile(outputFile)
		
		val revNodes1 = (xml \\ "query" \ "pages" \ "page" \ "revisions" \ "rev")
		
				// Loop through each xml node
	    val n = revNodes1.head
		
		val revID = n \ "@revid"
		
		// Start a RDF node
		
		var resultNode:String =  PrefixWB + "\n" + PrefixXSD // PrefixBase +
				
		
		// Node id :revision/Vienna/607920704 
		resultNode = resultNode +  RdfRevisionStart.format(title, revID)
		  
		var nodeContent:String = createRevisionNode(title, revID.mkString, n)
		resultNode = resultNode + nodeContent
		
		resultNode = resultNode + RdfRevisionEnd
		
		return resultNode
		
	}
	// 
	def createRevisionNode(title : String, revID : String, n : scala.xml.NodeSeq) : String = { 
	
		  var resultNode:String = ""
		
		  var resourceTitle = "http://en.dbpedia.org/resource/" + title
		  var resourceTitleWiki = "http://en.wikipedia.org/" + title // resource/
		  	  
		  // Create metadata triples
		  resultNode = resultNode + " wb:revid %s ;" .format(revID)

		  resultNode = resultNode + " wb:parentid %s ;" .format(n \ "@parentid")
		  resultNode = resultNode + " wb:sourceWiki <%s> ;".format(resourceTitleWiki)

		  // Reference to data :resource/Vienna/607920704 
		  resultNode = resultNode + " wb:data <http://data.wu.ac.at/wayback/dbpedia/%s/id/%s> ;" .format(title, revID) // <:%s/id/%s>
		  
		  resultNode = resultNode + " wb:user \"%s\" ;" .format(n \ "@user")
		  resultNode = resultNode + " wb:userid %s ;" .format(n \ "@userid")
		  resultNode = resultNode + " wb:timestamp \"%s\"^^xsd:dateTime ;" .format(n \ "@timestamp"  )
		  resultNode = resultNode + " wb:size %s ;" .format(n \ "@size")
		  resultNode = resultNode + " wb:sha1 \"%s\" ;" .format(n \ "@sha1")
		  // Not sure if we need this println(" :contentmodel %s ;" .format(n \ "@contentmodel"))

		  resultNode = resultNode + " wb:language \"en\";"
		  resultNode = resultNode + " wb:extractorFramework wb:DBpediaExtractionFramework;"
		  
		  var comment = (n \ "@comment").mkString
		  resultNode = resultNode +" wb:comment \"%s\" " .format(comment.filter(_ >= ' '))
		  
		  return resultNode
	
		
	}
	
	def getResourceById(title : String, revisionId : String, format : String, clean : Boolean, originalURI : Boolean) : String = {
            
		val folder = new File(download_directory)
		val ontologyFile =  ontology_name
		// val revision_id = revisionId
		
		var quads = isQuads(format)
				
		println("Start single extraction by ID for: " + title)
		
		var saveResult:String = ""
		
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)
		
		val fileName = pathName + "/" + revisionId + ".ttl" // prefixResFile + 
		
		val pathNameForFile: Path = Path.fromString(fileName)
		
		if(pathNameForFile.exists) {
			
			saveResult = Source.fromFile(fileName).getLines mkString "\n"		
			
		}
		else {
				
			val url = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=" + 1 + "&rvcontentformat=text%2Fx-wiki&rvstartid=" + revisionId + "&rvdir=older&exportnowrap=&titles="  + title
			
			val extractor = new ExtractLogic
		
			extractor.downloadAndCreateTemplate(pathName, title, url)
			
			if(quads==false) {
				saveResult = extractor.extractTriples(pathName, ontologyFile, revisionId, title, originalURI)
			}
			else {
				saveResult = extractor.extractQuads(pathName, ontologyFile, revisionId, title, true)
			}
	
		}
		
		return saveResult
		
    }	
	
	def getResourceByDate(title : String, revisionTime : String, format : String, clean : Boolean, originalURI : Boolean) : String = { // , revisionLimit : Int
            
		//val testDataRootDir = new File("./core/data/infobox_samples")
		//val title = args(0)
		val folder = new File(download_directory)
		val ontologyFile =  ontology_name
		val revision_time = revisionTime
		
		var quads = isQuads(format)
			
		println("Start single extraction by date for: " + title )
		
		val pathName = folder.getPath() + "/" + title
		val newPath: Path = Path.fromString(pathName)
		newPath.createDirectory(failIfExists=false)
		
		// Wikipedia Web API (check for details)
		val url = WikipediaAPI_Base + "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=" + revision_time + "&rvdir=older&exportnowrap=&titles="  + title // " + revision_limit + "

	
		println(url)
		
		var saveResult:String = ""		
		
		val extractor = new ExtractLogic
		
		// Download single revision
		val resultTuple = extractor.downloadAndCreateTemplate(pathName, title, url) // :Tuple2
		
		// For now just a single language
		//extractor.extractQuads(pathName, ontologyFile) //  , "fr"
		
		if(quads==false) {
			saveResult = extractor.extractTriples(pathName, ontologyFile, resultTuple._1, title, originalURI)
		}
		else {
			saveResult = extractor.extractQuads(pathName, ontologyFile, resultTuple._1, title, true)
		}
			
		
		
		return saveResult
	
    }	
	
}

class ExtractLogic  { // extends App
		
	 private val filter = new FilenameFilter
	{
		def accept(dir: File, name: String) = name endsWith ".xml"
	}
	
	private val formater = new TerseFormatter(true,true)
	private val parser = WikiParser.getInstance()
	
	def emptyDirectory(pathName : String){
		val path: Path = Path.fromString(pathName) // Path ()
		path.deleteRecursively(true)
		path.createDirectory()
		
	}

	def downloadAndCreateTemplate2(pathName : String, title : String, url : String): (String, String) = {


		val newFile: Path = Path.fromString(pathName + "/" + "download.xml")
		newFile.deleteIfExists()

		// Download and write it straight to temp file
		val outputFile = new File(pathName + "/" + "download.xml")  // Temp/
		new URL(url) #> outputFile !!


		// Extract revision ID from file
		val xml = XML.loadFile(outputFile)
		// val revID = (xml \ "page" \ "revision" \ "id").text
		val revNodes1 = (xml \\ "query" \ "pages" \ "page" \ "revisions" \ "rev")
		var revID = revNodes1.head \ "@revid"
		var content = scala.xml.Utility.escape(revNodes1.head.text)

		println("RevID " + revID)
		//println("Rev Content" + content)

		val fileName = pathName+ "/" + "page" + ".xml"
		val pathNameForFile: Path = Path.fromString(fileName)

		pathNameForFile.deleteIfExists()

		val wikiTemplate = "<mediawiki xml:lang=\"en\"> \n <page> <title>%s</title> <ns>0</ns> <id>0</id> \n <revision> <timestamp></timestamp> <contributor><ip>123</ip></contributor> <comment></comment> \n <text>%s</text> \n </revision> \n </page> \n </mediawiki>".format(title, content)

		printToFile2(fileName, wikiTemplate)

		//return wikiTemplate

		return (revID.toString, wikiTemplate)

		//val oldFile: Path = Path.fromString(pathName + "/" + "download.xml")
		//val newFile: Path = Path.fromString(pathName+ "/" + revID + ".xml")
		//val oldFile: Path = Path.fromString(pathName + "/Temp")
		//val newFile: Path = Path.fromString(pathName+ "/" + revID )
		//oldFile.moveTo (target=newFile)

	}

	
	def downloadAndCreateTemplate(pathName : String, title : String, url : String): (String, String) = { 
		
		
		val newFile: Path = Path.fromString(pathName + "/" + "download.xml")
		newFile.deleteIfExists()
		
		// Download and write it straight to temp file
		val outputFile = new File(pathName + "/" + "download.xml")  // Temp/
		new URL(url) #> outputFile !!
		
		
		// Extract revision ID from file
		val xml = XML.loadFile(outputFile)
		// val revID = (xml \ "page" \ "revision" \ "id").text
		val revNodes1 = (xml \\ "query" \ "pages" \ "page" \ "revisions" \ "rev")
		var revID = revNodes1.head \ "@revid" 
		var content = scala.xml.Utility.escape(revNodes1.head.text)
		
		println("RevID " + revID)
		//println("Rev Content" + content)
		
		val fileName = pathName+ "/" + revID + ".xml"
		val pathNameForFile: Path = Path.fromString(fileName)
		
		pathNameForFile.deleteIfExists()	

		val wikiTemplate = "<mediawiki xml:lang=\"en\"> \n <page> <title>%s</title> <ns>0</ns> <id>0</id> \n <revision> <timestamp></timestamp> <contributor><ip>123</ip></contributor> <comment></comment> \n <text>%s</text> \n </revision> \n </page> \n </mediawiki>".format(title, content)

		printToFile2(fileName, wikiTemplate)
		
		//return wikiTemplate
		
		return (revID.toString, wikiTemplate)
		
		//val oldFile: Path = Path.fromString(pathName + "/" + "download.xml")
		//val newFile: Path = Path.fromString(pathName+ "/" + revID + ".xml")
		//val oldFile: Path = Path.fromString(pathName + "/Temp")
		//val newFile: Path = Path.fromString(pathName+ "/" + revID )
		//oldFile.moveTo (target=newFile)
		
	}
	
	def extractTriples(pathName : String, _ontology : String, revisionID : String, title : String, includeRev : Boolean): String = { // : File
		
		val _language =  Language.English // English
		
		val context = new {
			def ontology = {
					//val ontoFilePath = "../ontology.xml"
					val ontoFile =  new File(_ontology)
					val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
					new OntologyReader().read(ontologySource)
			}
			def language = _language
			def redirects = new Redirects(Map())
		}

		var fileName = revisionID + ".xml" //  pathName + "/" + 
		val folder = new File(pathName)
		
		val result = writeTriplesToFile(fileName, context, folder, revisionID, title, includeRev)
		
		return result
		
	}

	def extractQuads2(pathName : String, _ontology : String, revisionID : String, title : String, includeRev : Boolean):  scala.collection.mutable.HashMap[PropertyNode, Quad] = { // : File

		// val folder = new File(pathName)

		// println("Read folder " + folder.getAbsolutePath())

		val _language =  Language.English // English

		val context = new {
			def ontology = {
				val ontoFile =  new File(_ontology)
				val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
				new OntologyReader().read(ontologySource)
			}
			def language = _language
			def redirects = new Redirects(Map())
		}

		//for(f <- folder.listFiles(filter) )
		//{
		//	writeQuadsToFile(f.getName, context, folder)
		//}

		var fileName = revisionID + ".xml" //  pathName + "/" +
		val folder = new File(pathName)

		val result = writeQuadsToFile2(fileName, context, folder, revisionID, title, includeRev)
		return result
	}
	
	
	def extractQuads(pathName : String, _ontology : String, revisionID : String, title : String, includeRev : Boolean): String = { // : File
		
		// val folder = new File(pathName)
		
		// println("Read folder " + folder.getAbsolutePath())

		val _language =  Language.English // English
		
		val context = new {
			def ontology = {
					val ontoFile =  new File(_ontology)
					val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
					new OntologyReader().read(ontologySource)
			}
			def language = _language
			def redirects = new Redirects(Map())
		}

		//for(f <- folder.listFiles(filter) )
		//{
		//	writeQuadsToFile(f.getName, context, folder)
		//}
		
		var fileName = revisionID + ".xml" //  pathName + "/" + 
		val folder = new File(pathName)
		
		val result = writeQuadsToFile(fileName, context, folder, revisionID, title, includeRev)
		
		return result
		
		
	}

	def writeQuadsToFile2(fileNameWiki : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder:File, revisionID : String, title : String, includeRev : Boolean):  scala.collection.mutable.HashMap[PropertyNode, Quad] =  {

		val outputFileName = folder.getPath() + "/" + fileNameWiki.replace(".xml",".ttl")

		// Delete file if it exists
		val pathNameForFile: Path = Path.fromString(outputFileName)
		pathNameForFile.deleteIfExists()


		//println("Transform wiki "+fileNameWiki+" and output "+goldFile)
		val resultSeq = render2(fileNameWiki, context, folder)

		return resultSeq

	}


	def writeQuadsToFile(fileNameWiki : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder:File, revisionID : String, title : String, includeRev : Boolean): String =  {
			
		val outputFileName = folder.getPath() + "/" + fileNameWiki.replace(".xml",".ttl")

		// Delete file if it exists
		val pathNameForFile: Path = Path.fromString(outputFileName)
		pathNameForFile.deleteIfExists()	
		
		
		//println("Transform wiki "+fileNameWiki+" and output "+goldFile)
		val resultSeq = render(fileNameWiki, context, folder).map(formater.render(_).trim()).toSet
		
		var resultString:String = ""
		
		for( p <- resultSeq){
			
			// Filter Properties (population type property) and Labels (population label "abc"), since they already in the ontology
			val posType = p.indexOf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
			val posLabel = p.indexOf("http://www.w3.org/2000/01/rdf-schema#label")
			if(posType < 0 &&  posLabel < 0)  { // dbpedia.org/property/
			
				// Make 4 quad smaller (e.g. <http://en.wikipedia.org/wiki/Vienna?oldid=663769963#section=Geography_and_climate&relative-line=28&absolute-line=219> to <http://en.wikipedia.org/wiki/Vienna?oldid=663769963>)
				val posHash = p.lastIndexOf("<http://en.wikipedia.org") // Begin of quad
				val posLarger = p.lastIndexOf(">") // end of quad
				var p2 = p
				
				if(posHash > 0) {
					val graphName = "<http://data.wu.ac.at/wayback/dbpedia/%s/revision/id/%s>" .format(title, revisionID)
					p2 = p.replace(p.slice(posHash, posLarger+1), graphName) // posLarger-1 ""
				}
				
				resultString = resultString + p2 + "\n"
				
				//println(p2)
	   			printToFile2(outputFileName, p2)
			
			}
		}
		
		return resultString

	}
	
	
	def writeTriplesToFile(fileNameWiki : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder : File, revisionID : String, title : String, includeRev : Boolean): String =  {
		
		val outputFileName = folder.getPath() + "/" + fileNameWiki.replace(".xml",".ttl") 
		
		// Delete file if it exists
		val pathNameForFile: Path = Path.fromString(outputFileName)
		pathNameForFile.deleteIfExists()	
		
		
		//println("Transform wiki "+fileNameWiki+" and output "+goldFile)
		val resultSeq = render(fileNameWiki, context, folder).map(formater.render(_).trim()).toSet
				
		var resultList = new ListBuffer[String]()  // [String]
		
		// Add hasRevision
		resultList += " wb:hasRevision :%s/revision/id/%s ;".format(title, revisionID); 
		
		for( p <- resultSeq){
			
			// Filter Properties (population type property) and Labels (population label "abc"), since they already in the ontology
			val posType = p.indexOf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
			val posLabel = p.indexOf("http://www.w3.org/2000/01/rdf-schema#label")

			if(posType < 0 &&  posLabel < 0)  { // dbpedia.org/property/
			
				// Remove quad to make triple
				val posLastSmaller = p.lastIndexOf("<")
				val posLastLarger = p.lastIndexOf(">")
				var p2 = p
				
				if(posLastSmaller > 0) {
					p2 = p.replace(p.slice(posLastSmaller, posLastLarger+1),"")
				}
				
				var p3 = p2.replace("/" + title , "/" + title + "/" +  revisionID)
					
				var resultString1:String = ""
			
				if(includeRev) {
					// Move revision id into object (http://en.dbpedia.org/resource/Madrid/609572729)
					//resultString = resultString + p3 + "\n"
					resultString1 = p3
				}
				else {
					// Important for the processing on higher levels the revision id (e.g. /Madrid/609572729) is 
					// not in the uri (e.g. /Madrid) of the subject -> Use p2 instead of p3
					//resultString = resultString + p2 + "\n"
					resultString1 = p2
				}
				
				resultList += resultString1; 
				
			}
		}
		
		// Sort
		// resultList.sortWith(_ < _)

		var resultString2:String = ""
		
		for( str <- resultList.sorted){
			
			// Create string for API
			resultString2 = resultString2 + str + "\n"
			
			// Write to files system for caching
	   		printToFile2(outputFileName, str) // p3
				
		}
		
		return resultString2

	}


	private def render2(file : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder : File) : scala.collection.mutable.HashMap[PropertyNode, Quad] =
	{
		val extractor = new InfoboxExtractor(context)

		println("Input file : " + folder + "/" + file)
		val page = //new FileSource(folder, context.language, _ endsWith file).head
			XMLSource.fromFile(new File(folder.getPath() + "/" + file),context.language).head
		println("resourceIri : " + page.title.resourceIri)

		parser(page) match {
			case Some(n) => extractor.extractInfoboxFactsToQuads(n,page.title.resourceIri,new PageContext())
			case None => null
		}

	}

	private def render(file : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder : File) : Seq[Quad] =
	{
		val extractor = new InfoboxExtractor(context)

		println("Input file : " + folder + "/" + file)
		val page = //new FileSource(folder, context.language, _ endsWith file).head
		  XMLSource.fromFile(new File(folder.getPath() + "/" + file),context.language).head
		println("resourceIri : " + page.title.resourceIri)

    	parser(page) match {
      		case Some(n) => extractor.extract(n,page.title.resourceIri,new PageContext())
      		case None => Seq.empty
    	}

    }
	
	def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
	  val p = new java.io.PrintWriter(f)
	  try { op(p) } 
	  finally { p.close() }
    }
	
	def printToFile2(f: String, s: String): Unit = {
    	val pw = new java.io.PrintWriter(new java.io.FileWriter(f, true))
    	try { pw.println(s) }
		finally { pw.close() }
	}
	
	
}    




}