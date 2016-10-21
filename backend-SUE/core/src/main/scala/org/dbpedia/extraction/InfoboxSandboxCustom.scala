package org.dbpedia.extraction

import java.io._
import java.util
import java.util.NoSuchElementException
import javax.swing._
import javax.xml.stream.XMLStreamException

import com.hp.hpl.jena.query._
import com.hp.hpl.jena.sparql.core.BasicPattern
import com.hp.hpl.jena.sparql.modify.request.UpdateModify
import com.hp.hpl.jena.tdb.TDBFactory
import com.hp.hpl.jena.update.{UpdateFactory, UpdateRequest}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import wikiPropertiesRecommendation.WikiTemplateManager

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashMap, MultiMap, Set}
import scalax.file.Path


//import org.junit.{Ignore, Test}
import at.tuwien.dbai.rewriter._
import at.tuwien.dbai.rewriter.incons._
import org.dbpedia.extraction.ontology.Ontology

import scala.language.reflectiveCalls

class Context(){

}
class InfoboxSandboxCustom(var testDataRootDir:File, var mappingFileSuffix:String,downloadDirectory: String="./data/downloads", ontologyName: String= "ontology.xml", mapping_Path: String= "mappings") {
  var update: Tuple4[java.lang.String, java.lang.String, java.lang.String, java.lang.String] = null
  private var chosenInfoboxUpdate: Seq[WikiDML] = null
  private var download_directory = downloadDirectory
  private var ontologyPath = ontologyName
  private var mappingsPath = mapping_Path
  //private var download_directory = "./data/downloads"
  //private var ontologyPath = "ontology.xml"
  //private var mappingsPath = "mappings"
  private val placeholder = "$$$"
  private val parser = WikiParser.getInstance()


  // can't we call a function for this?

  val ontoFilePath = ontologyPath
  println(ontoFilePath)
  val ontoFile = new File(ontoFilePath)
  val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
  val ontoObj = new OntologyReader().read(ontologySource)

  private var context = new {

    def ontology = ontoObj

    def language = Language.English

    def mappingPageSource = {
      val namespace = Namespace.mappings(language)

      val file = new File(mappingsPath,
        namespace.name(Language.Mappings).replace(' ', '_') +
          mappingFileSuffix)

      XMLSource.fromFile(file, Language.Mappings)

    }

    def redirects = new Redirects(Map())

  }
  //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
  // Loading mappings and redirects
  //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

  private var contextMappings = new {
   // println("context create!")
    def mappings: Mappings = MappingsLoader.load(context)
    def temp=mappings.templateMappings
    def redirects: Redirects = new Redirects(Map())
  }
  private var extractor =  new MappingExtractor(contextMappings)


  def setConfiguration(downloadDirectory: String, ontologyName: String, mapping_Path: String) {
    download_directory = downloadDirectory
    ontologyPath = ontologyName
    mappingsPath = mapping_Path
  }




  def setUpdate(tp: Tuple4[java.lang.String, java.lang.String, java.lang.String, java.lang.String]) {
    update = tp
  }

  def resolveForLanguageFromUI() = {
   // println("download_directory:" + download_directory + "/Santi_Cazorla/705145701.xml")
    val testDataRootDir = new File(download_directory + "/Santi_Cazorla/705145701.xml")
    resolveForLanguage(testDataRootDir, Language.English)
  }

  /**
   * Resolves a ground update to a set of sets of WikiDMLs
    *
    * @param file wiki page
   * @param _language langage of mappings
   * @return
   */
  def resolveForLanguage(file: File, _language: Language) = {

  //  println("tesft file " + file.getName())

    val ontoFilePath = ontologyPath
    val ontoFile = new File(ontoFilePath)
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
    val ontoObj = new OntologyReader().read(ontologySource)

    val context = new {

      def ontology = ontoObj

      def language = _language

      def mappingPageSource = {
        val namespace = Namespace.mappings(language)

        val file = new File(mappingsPath,
          namespace.name(Language.Mappings).replace(' ', '_') +
            mappingFileSuffix)

        XMLSource.fromFile(file, Language.Mappings)

      }

      def redirects = new Redirects(Map())

    }

    resolver(file.getName, context, file)
  }

  def checkConsistencyFromUpdate(updateQuery: String) = {
    val update = UpdateFactory.create(updateQuery)

    //  val test = new InfoboxSandboxCustom()
    println(checkConsistency(update))
  }

  /**
   * Main entry function: Renders a wiki page to a set of Quads
    *
    * @param file wiki page
   * @param _language language of the mappings
   * @return Set of Quads
   */
  def renderWithChangesForLanguage(file: File, _language: Language): Seq[Quad] = {

    //println("test file " + file.getName())

    val ontoFilePath = ontologyPath
    val ontoFile = new File(ontoFilePath)
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
    val ontoObj = new OntologyReader().read(ontologySource)

    val context = new {

      def ontology = ontoObj

      def language = _language

      def mappingPageSource = {
        val namespace = Namespace.mappings(language)

        val file = new File(mappingsPath,
          namespace.name(Language.Mappings).replace(' ', '_') + mappingFileSuffix)
        XMLSource.fromFile(file, Language.Mappings)

      }

      def redirects = new Redirects(Map())

    }

    return renderWithChanges(file.getName, context, file)
  }

  /**
   * Renders a wiki page to a set of Quads
    *
    * @param file the wiki page
   * @param context the mappings and ontology
   * @param folder folder of the wiki page
   * @return
   */
  private def renderWithChanges(file: String, context: AnyRef {def ontology: Ontology; def language: Language; def redirects: Redirects; def mappingPageSource: Traversable[WikiPage]}, folder: File): Seq[Quad] = {
    var result: Seq[Quad] = Seq.empty
/*
    val contextMappings = new {
      def mappings: Mappings = MappingsLoader.load(context)

      def redirects: Redirects = new Redirects(Map())
    }

    val extractor = new MappingExtractor(contextMappings)
*/
    //    println("input file : " + folder + "/" + file)
    val page = //new FileSource(folder, context.language, _ endsWith file).head
      XMLSource.fromFile(new File(folder.getPath()), context.language).head
    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(n) =>
        result ++=
          extractor.extractWithInfoboxChanges(n, page.title.resourceIri, new PageContext(), chosenInfoboxUpdate)
      case None => Seq.empty
    }

    result
  }

  /**
   * Main entry function: Renders a wiki page to a set of Quads
    *
    * @param file wiki page
   * @param _language language of the mappings
   * @return Set of Quads
   */
  def renderForLanguage(file: File, _language: Language): Seq[Quad] = {

    //println("test file " + file.getName())

    val ontoFilePath = ontologyPath
    val ontoFile = new File(ontoFilePath)
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
    val ontoObj = new OntologyReader().read(ontologySource)

    val context = new {

      def ontology = ontoObj

      def language = _language

      def mappingPageSource = {
        val namespace = Namespace.mappings(language)

        val file = new File(mappingsPath,
          namespace.name(Language.Mappings).replace(' ', '_') + mappingFileSuffix)
        XMLSource.fromFile(file, Language.Mappings)

      }

      def redirects = new Redirects(Map())

    }

    return render(file.getName, context, file)
  }

  /**
   * Renders a wiki page to a set of Quads
    *
    * @param file the wiki page
   * @param context the mappings and ontology
   * @param folder folder of the wiki page
   * @return
   */
  private def render(file: String, context: AnyRef {def ontology: Ontology; def language: Language; def redirects: Redirects; def mappingPageSource: Traversable[WikiPage]}, folder: File): Seq[Quad] = {
    var result: Seq[Quad] = Seq.empty

    /*val contextMappings = new {
      def mappings: Mappings = MappingsLoader.load(context)

      def redirects: Redirects = new Redirects(Map())
    }

    val extractor = new MappingExtractor(contextMappings)
*/
    //    println("input file : " + folder + "/" + file)
    val page = //new FileSource(folder, context.language, _ endsWith file).head
      XMLSource.fromFile(new File(folder.getPath()), context.language).head
    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(n) =>

        result = extractor.extract(n, page.title.resourceIri, new PageContext())

      case None => Seq.empty
    }

    result
  }


  /**
   * Resolves a ground triple Tuple4 of an instantiated SPARQL update
    *
    * @param file the name of the file of the wiki page
   * @param context mappings fixme: no need for ontology?
   * @param folder the folder of the file
   * @return a Set of Sets of WikiDMLs
   */
  private def resolver(file: String, context: AnyRef {def ontology: Ontology; def language: Language; def redirects: Redirects; def mappingPageSource: Traversable[WikiPage]}, folder: File): ArrayBuffer[Seq[WikiDML]] = {

    val wikiDML = new ArrayBuffer[Seq[WikiDML]]

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Loading mappings and redirects
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

   /* val contextMappings = new {
      def mappings: Mappings = MappingsLoader.load(context)

      def redirects: Redirects = new Redirects(Map())
    }

    val extractor = new MappingExtractor(contextMappings)
*/
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Loading a page from a XML file
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //    println("input file : " + folder + "/" + file)
    val page =
      XMLSource.fromFile(new File(folder.getPath()), context.language).head
    //    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(n) =>

        if (update._4 == "INSERT") {
          for (templateMapping <- contextMappings.mappings.templateMappings) {

            for (i <- 0 until templateMapping._2.asInstanceOf[ConditionalMapping].cases.size) {
              val condCase = templateMapping._2.asInstanceOf[ConditionalMapping].cases(i)

              for (propertyMapping <- condCase.mapping.asInstanceOf[TemplateMapping].mappings) {

                if ((propertyMapping.asInstanceOf[ConstantMapping].ontologyProperty.uri == update._2)
                  && (propertyMapping.asInstanceOf[ConstantMapping].value == update._3)) {

                  for {template <- collectTemplates(n)
                       resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                  } {
                    if (templateMapping._1.toUpperCase == resolvedTitle.toUpperCase) {

                      // TODO: other conditions here
                      val condDML = new ArrayBuffer[WikiDML]
                      if (condCase.operator == "isSet") {
                        condDML += new WikiDML(n.title.decoded, resolvedTitle, condCase.templateProperty, newValue = placeholder, operation = "INSERT")

                        for (j <- 0 until i) {

                          val condDisabled = templateMapping._2.asInstanceOf[ConditionalMapping].cases(j)

                          condDML += new WikiDML(n.title.decoded, resolvedTitle, condDisabled.templateProperty, operation = "DELETE")

                        }

                      }
                      wikiDML += condDML.toList

                    }

                  }

                }
              }

            }

            for (mapping <- templateMapping._2.asInstanceOf[ConditionalMapping].defaultMappings) {

              val templateProperty = mapping.asInstanceOf[SimplePropertyMapping].templateProperty
              val ontologyProperty = mapping.asInstanceOf[SimplePropertyMapping].ontologyProperty

              println(templateProperty + " --> " + ontologyProperty)

              if (ontologyProperty.uri == update._2) {

                for {template <- collectTemplates(n)
                     resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                } {
                  //println(templateMapping._1 + " == " + resolvedTitle)
                  if (templateMapping._1.toUpperCase == resolvedTitle.toUpperCase)
                    wikiDML += Seq(new WikiDML(n.title.decoded, resolvedTitle, templateProperty, newValue = update._3, operation = "INSERT"))
                }

              }
            }
          }
        }
        else if (update._4 == "DELETE") {

          extractor.resolve(n, subjectUri = update._1, context = new PageContext(),
            updateSubjectUri = null, update._2, update._3, update._4)
        }

      case None => Seq.empty
    }

    wikiDML
  }

  def countInfoboxPropertiesWithGaps(titles: Seq[String], infoboxProperties: Seq[WikiDML]) = {

    var infoboxTitle = infoboxProperties(0).infobox.toUpperCase()
    var gaps = new Array[Int](infoboxProperties.size)


    for (title <- titles) {
      println(title)
      // TODO: check if title exists in the specified path

      // get all properties from titles
      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val url = "https://en.wikipedia.org/w/api.php?" +
        "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

      val extractLogic = new ExtractLogic

      try {

        if (!new File(download_directory + "/" + title + "/page.xml").exists()) {
          val (revID, template) = extractLogic.downloadAndCreateTemplate2(pathName, title, url)
        }

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/page.xml"), Language.English).head

        parser(page) match {
          case Some(node) =>

            val infoboxes = collectTemplates(node)
            val filteredInfoboxes = infoboxes.filter(x => infoboxTitle.contains(x.title.decoded.toUpperCase)) // filter infoboxes by subject

            for (infobox <- filteredInfoboxes) {
              for (i <- 0 until infoboxProperties.size) {
                for (j <- i + 1 until infoboxProperties.size) {
                  if (!infobox.keySet.contains(infoboxProperties(i).property) &&
                            infobox.keySet.contains(infoboxProperties(j).property))
                    gaps(i) += 1
                }
              }

            }
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
        case _: XMLStreamException => println("XML page could not be read:" + pathName)
      }

    }
    gaps

  }

  def countInfoboxProperties(titles: Seq[String], infoboxProperties: Seq[WikiDML]) = {

    var infoboxTitles = new ArrayBuffer[String] // used for filtering infoboxes based on titles by wikiDML
    val infoboxCount = new scala.collection.mutable.HashMap[String, Int]

    // initialize infoboxKeys with 0s
    for (wikiDML <- infoboxProperties) {
      infoboxCount += (wikiDML.infobox + ":" + wikiDML.property -> 0) // this is what we need to count and return
      if (!infoboxTitles.contains(wikiDML.infobox.toUpperCase))
        infoboxTitles += wikiDML.infobox.toUpperCase
    }

   // println("Counting Infobox properties...")
    for (title <- titles) {
//      println(title)

      // get all properties from titles
      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val url = "https://en.wikipedia.org/w/api.php?" +
        "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

      val extractLogic = new ExtractLogic

      try {

        if (!new File(download_directory + "/" + title + "/page.xml").exists()) {
          val (revID, template) = extractLogic.downloadAndCreateTemplate2(pathName, title, url)
        }

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/page.xml"), Language.English).head

        parser(page) match {
          case Some(node) =>

            val infoboxes = collectTemplates(node)
            val filteredInfoboxes = infoboxes.filter(x => infoboxTitles.contains(x.title.decoded.toUpperCase)) // filter infoboxes by subject

            for (infobox <- filteredInfoboxes) {
              for ((k, v) <- infoboxCount) {
                // enumerate all keysets
                if (infobox.keySet.contains(k.substring(k.indexOf(":") + 1)))
                  infoboxCount(k) += 1 // add +1 to keysets
              }
            }
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
        case _: XMLStreamException => println("XML page could not be read:" + pathName)
      }
    }

    infoboxCount

  }

  // reads and writes statistics using a file with pages
  def countInfoboxProperties(file: String, infoboxProperties: Seq[WikiDML], title: String) = {

    var oldInfoboxCount = new scala.collection.mutable.HashMap[String, Int]

    /* Load the stats hashmap */
    try {
      val fileIn = new FileInputStream("./data/downloads/infobox-count-" + title + ".ser")
      val in = new ObjectInputStream(fileIn)
      oldInfoboxCount = in.readObject().asInstanceOf[scala.collection.mutable.HashMap[String, Int]]
      println("Old hashmap:" + oldInfoboxCount)
      in.close
      fileIn.close
    }
    catch {
      case _: Exception => {}
    }

    var newInfoboxTitles = new ArrayBuffer[String] // used for filtering infoboxes based on titles by wikiDML

    // initialize new infoboxKeys with 0s
    var newInfoboxCount = new scala.collection.mutable.HashMap[String, Int]
    for (wikiDML <- infoboxProperties) {
      if (!oldInfoboxCount.contains(wikiDML.infobox + ":" + wikiDML.property)) {
        newInfoboxCount += (wikiDML.infobox + ":" + wikiDML.property -> 0) // this is what we need to count and return
        newInfoboxTitles += wikiDML.infobox
      }
    }

    // get all properties from titles
    val pathName = download_directory + file

    /* Process only new ones */
    try {
      val pages = XMLSource.fromFile(new File(pathName), Language.English)
      //      println("Pages loaded!")
      println("Dataset name: " + file)
      println("Dataset size: " + pages.size)

      for (page <- pages) {
        parser(page) match {

          case Some(node) =>

            val infoboxes = collectTemplates(node)

            val filteredInfoboxes = infoboxes.filter(x => newInfoboxTitles.contains(x.title.decoded)) // filter infoboxes by subject

            for (infobox <- filteredInfoboxes) {
              for ((k, v) <- newInfoboxCount) {
                if (infobox.keySet.contains(k.substring(k.indexOf(":") + 1))) // strip off only the property name {
                  newInfoboxCount(k) += 1 // add +1 to keysets
              }
            }
        }
      }
    }
    catch {
      case _: NoSuchElementException => println("Bad path name: " + pathName)
    }

    val res = oldInfoboxCount ++ newInfoboxCount

    try {
      val fileOut = new FileOutputStream("./data/downloads/infobox-count-" + title + ".ser")
      val out = new ObjectOutputStream(fileOut)
      out.writeObject(res)
      out.close
      fileOut.close
    }
    catch {
      case _: NoSuchElementException => println("Bad file path")
    }

    res

  }


  def resolveUpdate(update: UpdateRequest): (ArrayBuffer[ArrayBuffer[Seq[WikiDML]]], ArrayBuffer[String]) = {
    val wikiDML = new ArrayBuffer[ArrayBuffer[Seq[WikiDML]]]
    val titles = new ArrayBuffer[String]

    //println("groupbysubject")
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Loading a page from a XML file
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    val groupedUpdate = groupUpdateBySubject(update)

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Resolve DELETEs
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    for ((subject, quads) <- groupedUpdate._1) {
      // take deletes

      val subjwikiDML = new ArrayBuffer[Seq[WikiDML]]

      // download the page and do extraction of quads
      var title = subject.toString

      if (title.indexOf("/") != -1)
        title = title.substring(title.lastIndexOf("/") + 1)

      titles += title

      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val url = "https://en.wikipedia.org/w/api.php?" +
        "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

      val extractLogic = new ExtractLogic

      try {

        val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, title, url)

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/" + revID + ".xml"), Language.English).head

        parser(page) match {
          case Some(n) =>

            for (update <- quads) {

              val triple = update.asTriple
              subjwikiDML += extractor.resolve(n, subjectUri = triple.getSubject.toString, context = new PageContext(),
                updateSubjectUri = null, triple.getPredicate.toString, triple.getObject.toString, "DELETE")
              println(wikiDML)
            }

          case None => Seq.empty
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
      }
      wikiDML += subjwikiDML

    }

   // println("Resolve INSERTs")

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Resolve INSERTs
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    for ((subject, quads) <- groupedUpdate._2)
    {

      val subjwikiDML = new ArrayBuffer[Seq[WikiDML]]

      // download the page and do extraction of quads
      var title = subject.toString

      if (title.indexOf("/") != -1)
      {
        title = title.substring(title.lastIndexOf("/") + 1)
      }

      titles += title

      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val url = "https://en.wikipedia.org/w/api.php?" +
        "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

      // println("Create Logic")
      val extractLogic = new ExtractLogic

      try
      {

       // println("Extract Logic")
        val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, title, url)

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/" + revID + ".xml"), Language.English).head

        parser(page) match
        {
          case Some(n) =>

            for (update <- quads)
            {

              val triple = update.asTriple
              val tSubject = triple.getSubject.toString
              val tPredicate = triple.getPredicate.toString
              val tObject = triple.getObject.toString

              val templateNames = collectTemplateNames(n)
              //println("For each templateMapping")
              // for (templateMapping <- contextMappings.mappings.templateMappings) {
              for (templateMapping <- contextMappings.temp.filter(x => collectTemplateNames(n).contains(x._1.toLowerCase))
              )
              {
                //println("insider")
                if (templateMapping._2.isInstanceOf[ConditionalMapping])
                {

                  for (i <- 0 until templateMapping._2.asInstanceOf[ConditionalMapping].cases.size)
                  {

                    val condCase = templateMapping._2.asInstanceOf[ConditionalMapping].cases(i)

                    for (propertyMapping <- condCase.mapping.asInstanceOf[TemplateMapping].mappings)
                    {

                      if (propertyMapping.isInstanceOf[ConstantMapping])
                      {

                        if ((propertyMapping.asInstanceOf[ConstantMapping].ontologyProperty.uri == tPredicate)
                          && (propertyMapping.asInstanceOf[ConstantMapping].value == tObject))
                        {

                          for {template <- collectTemplates(n)
                               resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                          }
                          {
                            if (templateMapping._1.toUpperCase == resolvedTitle.toUpperCase)
                            {

                              // TODO: other conditions here
                              val condDML = new ArrayBuffer[WikiDML]
                              if (condCase.operator == "isSet")
                              {

                                condDML += new WikiDML(n.title.decoded, resolvedTitle, condCase.templateProperty, newValue = placeholder, operation = "INSERT")

                                for (j <- 0 until i)
                                {

                                  val condDisabled = templateMapping._2.asInstanceOf[ConditionalMapping].cases(j)

                                  condDML += new WikiDML(n.title.decoded, resolvedTitle, condDisabled.templateProperty, operation = "DELETE")

                                }

                              }

                              subjwikiDML += condDML.toList

                            }

                          }

                        }
                      }
                      // end of "if" constant mapping

                    }
                    // end of "for" all mappings in conditional case
                  }
                  // end of "for" all cases in conditional mappings
                }
                // end of "if" mapping is conditional mapping

                if (templateMapping._2.isInstanceOf[TemplateMapping])
                {

                  for (mapping <- templateMapping._2.asInstanceOf[TemplateMapping].mappings)
                  {

                    if (mapping.isInstanceOf[SimplePropertyMapping])
                    {

                      // TODO: ConstantMapping?
                      val templateProperty = mapping.asInstanceOf[SimplePropertyMapping].templateProperty
                      val ontologyProperty = mapping.asInstanceOf[SimplePropertyMapping].ontologyProperty

                      if (ontologyProperty.uri == tPredicate)
                      {

                        for {
                          template <- collectTemplates(n)
                          resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                        }
                        {
                          //println(templateMapping._1 + " == " + resolvedTitle)
                          if (templateMapping._1.toUpperCase == resolvedTitle.toUpperCase)
                            subjwikiDML += Seq(new WikiDML(n.title.decoded, resolvedTitle, templateProperty, newValue = tObject, operation = "INSERT"))
                        }

                      }
                    }
                  }
                }

                if (templateMapping._2.isInstanceOf[ConditionalMapping])
                {
                  for (mapping <- templateMapping._2.asInstanceOf[ConditionalMapping].defaultMappings)
                  {

                    if (mapping.isInstanceOf[SimplePropertyMapping])
                    {

                      val templateProperty = mapping.asInstanceOf[SimplePropertyMapping].templateProperty
                      val ontologyProperty = mapping.asInstanceOf[SimplePropertyMapping].ontologyProperty

                     // println(templateProperty + " --> " + ontologyProperty)

                      if (ontologyProperty.uri == tPredicate)
                      {

                        //               println(collectTemplates(n).size)
                        for {template <- collectTemplates(n)
                             resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                        }
                        {
                          //println(templateMapping._1 + " == " + resolvedTitle)
                          if (templateMapping._1.toUpperCase == resolvedTitle.toUpperCase)
                            subjwikiDML += Seq(new WikiDML(n.title.decoded, resolvedTitle, templateProperty, newValue = tObject, operation = "INSERT"))
                        }

                      }
                    }
                  }
                }
              }

            }

          case None => Seq.empty
        }
      }
      wikiDML += subjwikiDML

    }
    new Tuple2(wikiDML, titles)

  }

  /**
   * Collects all templates from a page
    *
    * @param node
   * @return
   */
  private def collectTemplates(node: Node): List[TemplateNode] = {
    node match {
      case templateNode: TemplateNode => List(templateNode)
      //case propertyNode: PropertyNode => List.empty
      case _ => node.children.flatMap(collectTemplates)
    }
  }

  private def collectTemplateNames(node: Node): ArrayBuffer[String] = {

    var result =  new ArrayBuffer[String]

    val templates = collectTemplates(node)

    for (template <- templates)
    {
      result +=  context.redirects.resolve(template.title).decoded.toLowerCase

    }

    result
  }


  def getDiffFromInfoboxUpdate_fromUI(wikiUpdate: Seq[WikiDML]) = {
    val testDataRootDir = new File(download_directory + "/Santi_Cazorla/705145701.xml")
    this.testDataRootDir = testDataRootDir
    getDiffFromInfoboxUpdate(wikiUpdate)
  }

  /**
   * fixme: Calculates the triples which are deleted and inserted
    *
    * @param wikiUpdate
   * @return
   */
  def getDiffFromInfoboxUpdate(wikiUpdate: Seq[WikiDML], wikiPage: String = null) = {
    //println("getDiffFromInfoboxUpdate")
    var oldView = new ArrayBuffer[Quad]()
    var newView = new ArrayBuffer[Quad]()

    if (wikiPage == null) {
      // require wikiUpdate(i).wikiPage == wikiUpdate(j).wikiPage
      oldView ++= extractQuads(wikiUpdate(0).wikiPage)
      newView ++= extractQuads(wikiUpdate(0).wikiPage, wikiUpdate)
    }
    else {
      oldView ++= extractQuads(wikiPage)
      newView ++= extractQuads(wikiPage, wikiUpdate)
    }

    var del = new ArrayBuffer[Quad]()
    var ins = new ArrayBuffer[Quad]()

    for (quad <- newView) {
      val subj = quad.subject
      val pred = quad.predicate
      val obj = quad.value

      var insQuad = false

      for (quad2 <- oldView) {

        val subj2 = quad2.subject
        val pred2 = quad2.predicate
        val obj2 = quad2.value

        if (subj == subj2 && pred == pred2 && obj == obj2)
          insQuad = true
      }

      if (!insQuad)
        ins += quad
    }

    for (quad <- oldView) {
      val subj = quad.subject
      val pred = quad.predicate
      val obj = quad.value

      var delQuad = false

      for (quad2 <- newView) {

        val subj2 = quad2.subject
        val pred2 = quad2.predicate
        val obj2 = quad2.value

        if (subj == subj2 && pred == pred2 && obj == obj2)
          delQuad = true
      }

      if (!delQuad)
        del += quad
    }

    new Tuple2(del, ins)

  }


  /**
   * Groups a SPARQL update by subject
    *
    * @param update
   * @return a Tuple2 of deletes and inserts
   */
  def groupUpdateBySubject(update: UpdateRequest) = {

    val subjectGroupsDEL = new HashMap[com.hp.hpl.jena.graph.Node, Set[com.hp.hpl.jena.sparql.core.Quad]]
      with MultiMap[com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.sparql.core.Quad]

    val subjectGroupsINS = new HashMap[com.hp.hpl.jena.graph.Node, Set[com.hp.hpl.jena.sparql.core.Quad]]
      with MultiMap[com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.sparql.core.Quad]

    val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]

    val deleteQuads = updateMod.getDeleteQuads
    val insertQuads = updateMod.getInsertQuads

    for (i <- 0 until insertQuads.size) {

      val subject = insertQuads.get(i).getSubject

      subjectGroupsINS.addBinding(subject, insertQuads.get(i))
    }

    for (i <- 0 until deleteQuads.size) {

      val subject = deleteQuads.get(i).getSubject

      subjectGroupsDEL.addBinding(subject, deleteQuads.get(i))

    }

    new Tuple2(subjectGroupsDEL, subjectGroupsINS)

  }

  /**
   * Loads Ontology of DBpedia and Mappings which use the former
    *
    * @return contextMappings
   */
  def loadOntologyAndMappings() = {

    val ontoFilePath = ontologyPath
    val ontoFile = new File(ontoFilePath)
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
    val ontoObj = new OntologyReader().read(ontologySource)

    // load the mappings
    val context = new {
      def ontology = ontoObj

      def language = Language.English

      def mappingPageSource = {
        val namespace = Namespace.mappings(language)

        val file = new File(mappingsPath, namespace.name(Language.Mappings).replace(' ', '_') + mappingFileSuffix)
        XMLSource.fromFile(file, Language.Mappings)
      }

      def redirects = new Redirects(Map())
    }

    val contextMappings = new {
      def mappings: Mappings = MappingsLoader.load(context)

      def redirects: Redirects = new Redirects(Map())
    }

    contextMappings
  }


  def checkConsistency(update: Seq[WikiDML], view: Seq[Quad] = null): Boolean = {

    var result: ArrayBuffer[Quad] = new ArrayBuffer[Quad]()

    var bConsistent = true

    if (view != null) {
      // reuse the view

      result ++= view

      // extract the subject
      var title = update(0).wikiPage

      if (title.indexOf("/") != -1)
        title = title.substring(title.lastIndexOf("/") + 1)

      val quads = extractQuads(title) // extract the quads from title

      result ++= quads // initialize with the quads generated by title

      val diff = getDiffFromInfoboxUpdate(update) // compute diff

      for (del <- diff._1) {

        var subj = del.subject
        if (subj.indexOf("/") != -1)
          subj = subj.substring(subj.lastIndexOf("/") + 1)

        val pred = del.predicate
        val obj = del.value

        for (i <- 0 to result.size - 1) {
          val quad = result(i)

          if (quad.subject == subj && quad.predicate == pred
            && quad.value == obj) // context?
            result.remove(i)
        }
      }
      result ++= diff._2
      //      result.toSet

      // serialize the quads to a file (remember Scala Quads can't be used in Jena)

    }
    else // start from scratch {
    // create a hashMap from a seq
    {
      val filtUpdate = new scala.collection.mutable.HashMap[String, ArrayBuffer[WikiDML]]

      for (upd <- update) {

        if (!filtUpdate.contains(upd.wikiPage)) {
          val list = new ArrayBuffer[WikiDML];
          list += upd
          filtUpdate += (upd.wikiPage -> list)
        }
        else {
          filtUpdate(upd.wikiPage) += upd
        }

      }

      for ((title, update) <- filtUpdate) {

        val quads = extractQuads(title, update) // extract the quads from title with changes

        result ++= quads

      }


      val t = new TerseFormatter(false, true)
      val extractLogic = new ExtractLogic

      val pathname = download_directory + "/" + "context.ttl"
      val file = new File(pathname);
      file.delete()

      for (quad <- result) {
        extractLogic.printToFile2(pathname, t.render(quad))
      }

      val dbpediaPath = download_directory + "/" + "dbpedia_2014.owl"

      val dataset = TDBFactory.createDataset(download_directory + "/" + "TDB")
      dataset.begin(ReadWrite.WRITE)
      val model = dataset.getDefaultModel()
      val ts = new TripleStore(model)
      ts.setDataset(dataset)

      ts.clear()
      ts.init(pathname, "N3")
      ts.init(dbpediaPath, "RDF/XML")

      ts.materializeDBpedia()

      val ts2 = new TripleStore(download_directory + "/" + "TDB", true)
      //  var dsRead = ts2.getDataset();
      // ts2.getDataset().begin(ReadWrite.READ);

      //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      //  Check for class disjoints inconsistency
      //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

      val queryConsistencyCheck =
        "SELECT ?subject \n" +
          "WHERE { { \n" +
          "?subject a ?type . \n" +
          "?subject a ?type2 . \n" +
          "?type <http://www.w3.org/2002/07/owl#disjointWith> ?type2 . } \n" +
          "UNION { ?subject ?predicate ?object . \n" +
          "?subject ?predicate2 ?object . \n" +
          "?predicate <http://www.w3.org/2002/07/owl#propertyDisjointWith> ?predicate2 }} "

      // todo: return the inconsistent triple
      val rs = ts2.getResultsFromQuery(queryConsistencyCheck);

      if (rs.hasNext) {
        while (rs.hasNext()) {
          rs.next()
        }
        ts2.getDataset().end()

        bConsistent = false
      }
      ts2.getDataset().end()
    }
    bConsistent
  }


  private def extractQuads(title: String, updates: Seq[WikiDML]): Seq[Quad] = {

    var sTitle = title

    if (sTitle.indexOf("/") != -1)
      sTitle = sTitle.substring(sTitle.lastIndexOf("/") + 1)

    var extractedQuads: Seq[Quad] = Seq.empty

    val folder = new File(download_directory)
   // val contextMappings = loadOntologyAndMappings()

    val pathName = folder.getPath() + "/" + sTitle
    val newPath: Path = Path.fromString(pathName)
    newPath.createDirectory(failIfExists = false)

    val url = "https://en.wikipedia.org/w/api.php?" +
      "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + sTitle // " + revision_limit + "

    val extractLogic = new ExtractLogic

    try {
      val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, sTitle, url)

      val page = XMLSource.fromFile(new File(download_directory + "/" + sTitle + "/" + revID + ".xml"), Language.English).head

    //  val extractor = new MappingExtractor(contextMappings)
      //  @Javi> mappings are in extractor, under templateMappings = context.mappings.templateMappings.
      parser(page) match {
        case Some(n) =>
          val subjectUri = "http://dbpedia.org/page/" + sTitle // fixme: add a method to check
          extractedQuads ++= extractor.extractWithInfoboxChanges(n, subjectUri, new PageContext(), updates)
        case None => Seq.empty
      }
    }
    catch {
      case _: NoSuchElementException => println("Bad path name: " + pathName)
    }

    extractedQuads


  }

  private def extractQuads(title: String): Seq[Quad] = {

    var sTitle = title

    if (sTitle.indexOf("/") != -1)
      sTitle = sTitle.substring(sTitle.lastIndexOf("/") + 1)

    var extractedQuads: Seq[Quad] = Seq.empty

    val folder = new File(download_directory)
    //val contextMappings = loadOntologyAndMappings()

    val pathName = folder.getPath() + "/" + sTitle
    val newPath: Path = Path.fromString(pathName)
    newPath.createDirectory(failIfExists = false)

    val url = "https://en.wikipedia.org/w/api.php?" +
      "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + sTitle // " + revision_limit + "

    val extractLogic = new ExtractLogic

    try {
      val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, sTitle, url)

      val page = XMLSource.fromFile(new File(download_directory + "/" + sTitle + "/" + revID + ".xml"), Language.English).head

     // val extractor = new MappingExtractor(contextMappings)
      //  @Javi> mappings are in extractor, under templateMappings = context.mappings.templateMappings.
      parser(page) match {
        case Some(n) =>
          val subjectUri = "http://dbpedia.org/page/" + sTitle
          extractedQuads ++= extractor.extract(n, subjectUri, new PageContext())
        case None => Seq.empty
      }
    }
    catch {
      case _: NoSuchElementException => println("Bad path name: " + pathName)
    }

    extractedQuads


  }


  /**
   * Checks if SPARQL update is consistent or not
    *
    * @param update
   * @return true if update is consistent
   */

  def checkConsistency(update: UpdateRequest): Boolean = {

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Group Update by subject
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    val subjectGroup = groupUpdateBySubject(update)
    val del = subjectGroup._1
    val ins = subjectGroup._2

    //val contextMappings = loadOntologyAndMappings()

    val folder = new File(download_directory)
    var subjQuads: collection.mutable.ArrayBuffer[Quad] = new ArrayBuffer[Quad]()

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Extract triples foreach subject
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    for ((subject, quads) <- ins) {

      // download the page and do extraction of quads
      var title = subject.toString

      if (title.indexOf("/") != -1)
        title = title.substring(title.lastIndexOf("/") + 1)

      val pathName = folder.getPath() + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val url = "https://en.wikipedia.org/w/api.php?" +
        "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

      val extractLogic = new ExtractLogic

      try {
        val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, title, url)

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/" + revID + ".xml"), Language.English).head

        //val extractor = new MappingExtractor(contextMappings)
        //  @Javi> mappings are in extractor, under templateMappings = context.mappings.templateMappings.
        parser(page) match {
          case Some(n) =>
            subjQuads ++= extractor.extract(n, subject.toString, new PageContext())
          case None => Seq.empty
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
      }
    }

    //  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Serialize the quads
    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    val t = new TerseFormatter(false, true)
    val extractLogic = new ExtractLogic

    // TODO: remove the file and start from scratch
    val pathname = download_directory + "/" + "context.ttl"
    val file = new File(pathname);
    file.delete()
    for (quad <- subjQuads) {
      extractLogic.printToFile2(pathname, t.render(quad))
    }

    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Perform the update
    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    val dbpediaPath = download_directory + "/" + "dbpedia_2014.owl"

    /*
    changed from:
    // val ts = new TripleStore(download_directory + "/" + "TDB")
    */
    val dataset = TDBFactory.createDataset(download_directory + "/" + "TDB")
    dataset.begin(ReadWrite.WRITE)
    val model = dataset.getDefaultModel()
    val ts = new TripleStore(model)
    ts.setDataset(dataset)

    ts.clear()
    ts.init(pathname, "N3")
    ts.init(dbpediaPath, "RDF/XML")

    ts.runUpdate(update)
    ts.materialize()

    // ts.getDataset().begin(ReadWrite.WRITE);

    ts.runUpdate(update)
    ts.materializeDBpedia()
    /*
    changed from:
    // ts.getDataset().commit()
    //ts.getDataset().end()
    //ts.getDataset().close()
    */
    ts.getDataset().getDefaultModel().commit()
    ts.getDataset().getDefaultModel().close()
    ts.getDataset().end()
    ts.getDataset().close()

    val ts2 = new TripleStore(download_directory + "/" + "TDB", true)
    //  var dsRead = ts2.getDataset();
    // ts2.getDataset().begin(ReadWrite.READ);

    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Check for class disjoints inconsistency
    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    val queryConsistencyCheck =
      "SELECT ?subject \n" +
        "WHERE { { \n" +
        "?subject a ?type . \n" +
        "?subject a ?type2 . \n" +
        "?type <http://www.w3.org/2002/07/owl#disjointWith> ?type2 . } \n" +
        "UNION { ?subject ?predicate ?object . \n" +
        "?subject ?predicate2 ?object . \n" +
        "?predicate <http://www.w3.org/2002/07/owl#propertyDisjointWith> ?predicate2 }} "

    // todo: return the inconsistent triple
    val rs = ts2.getResultsFromQuery(queryConsistencyCheck);

    if (rs.hasNext) {
      while (rs.hasNext()) {
        rs.next()
      }
      ts2.getDataset().end()

      return false
    }

    ts2.getDataset().end()


    return true

  }

  def getMappingType(title: String) = {

    val ontoFilePath = ontologyPath
    val ontoFile = new File(ontoFilePath)
    val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
    val ontoObj = new OntologyReader().read(ontologySource)

    val context = new {

      def ontology = ontoObj

      def language = Language.English

      def mappingPageSource = {
        val namespace = Namespace.mappings(language)

        val file = new File(mappingsPath,
          namespace.name(Language.Mappings).replace(' ', '_') +
            mappingFileSuffix)

        XMLSource.fromFile(file, Language.Mappings)
      }

      def redirects = new Redirects(Map())
    }

    val contextMappings = new {
      def mappings: Mappings = MappingsLoader.load(context)

      def redirects: Redirects = new Redirects(Map())
    }

    val pathName = download_directory + "/" + title
    val newPath: Path = Path.fromString(pathName)
    newPath.createDirectory(failIfExists = false)

    val url = "https://en.wikipedia.org/w/api.php?" +
      "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles=" + title // " + revision_limit + "

    val extractLogic = new ExtractLogic

    try {

      val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, title, url)

      val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/" + revID + ".xml"), Language.English).head

      parser(page) match {
        case Some(node) =>

          for {template <- collectTemplates(node)
               resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
          } {


            //                for (templateMapping <- contextMappings.mappings.templateMappings) {
            //
            //                for (i <- 0 until templateMapping._2.asInstanceOf[ConditionalMapping].cases.size) {

          }
      }
    }
    catch {
      case _: NoSuchElementException => println("Bad path name: " + pathName)
    }

  }

  def getQueryForResourcesWithSamePredicates(update: UpdateRequest, limit: Int, predicate: String, classType: String) = {

    val groupedAtomicUpdate = groupUpdateBySubject(update)
    val inserts = groupedAtomicUpdate._2

    val res = ArrayBuffer[String]()

    val dataset = "SELECT DISTINCT ?Y \n" +
      "WHERE { ?Y a " + classType + " . \n" +
      "?Y <" + predicate + "> ?Z2 . \n" +
      "} LIMIT " + limit
    res += dataset


    res
  }

  def getQueryResultsFromDBpedia(queryStr: String) =
  {
    var res: ResultSet = null
    val query = QueryFactory.create(queryStr)

    val qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)

    try {
      res = qexec.execSelect
    }
    finally {
      //qexec.close()
    }

    res

  }

  /**
   * Instantiates a general SPARQL update -- Where clause to live dbpedia
    *
    * @param update
   * @return Ground update
   */
  def instantiateGeneralUpdate(update: UpdateRequest): UpdateRequest = {

    val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

    val delete = updateMod.getDeleteQuads
    val insert = updateMod.getInsertQuads
    val where = updateMod.getWherePattern.toString

    val query = QueryFactory.create("SELECT * WHERE " + where)

    var atomicUpdate: UpdateRequest = null
    val qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://live.dbpedia.org/sparql", query)

    try {
      val results = qexec.execSelect
      val results2 = qexec.execSelect

      val delPattern = BasicPattern.wrap(UtilFunctions.convertQuadToTriples(delete))
      val delTriples = UtilFunctions.instantiateBGPs(delPattern, results)

      val insPattern = BasicPattern.wrap(UtilFunctions.convertQuadToTriples(insert))
      val insTriples = UtilFunctions.instantiateBGPs(insPattern, results2)

      var strUpdate = ""

      if (delTriples.size > 0)
        strUpdate += "DELETE { " + UtilFunctions.createUpdate(delTriples)

      if (insTriples.size > 0)
        strUpdate += "INSERT { " + UtilFunctions.createUpdate(insTriples)

      strUpdate += "WHERE {}"

      System.out.println(strUpdate)

      atomicUpdate = UtilFunctions.createUpdate(strUpdate)
    }
    finally {
      qexec.close()
    }

    return atomicUpdate
  }

  def updateFromUpdateQuery(updateQuery: String) = {
    //println("update!")
    val update = UpdateFactory.create(updateQuery)
    //println("create!")
    val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

    var atomicUpdate: UpdateRequest = null
    if (updateMod.getWherePattern.toString.contains("?")) {
      // general update contains one variable at least
      atomicUpdate = instantiateGeneralUpdate(update)
    }
    else
      atomicUpdate = update

    //println("resolveupdate!")
    resolveUpdate(atomicUpdate)
  }

  def getGroundTriplesFromUpdateQuery(updateQuery: String) = {
    val update = UpdateFactory.create(updateQuery)

    val ins = getGroundTriplesFromUpdate(update)._2
    setUpdate(ins(0))
  }


  /**
   * Transforms an update to a set of tuples of length 4, for both delete and insert
   * Tuple4(subject, predicate, object, operation)
    *
    * @param update
   * @return a Tuple2 for both delete and insert
   */
  def getGroundTriplesFromUpdate(update: UpdateRequest) = {

    var delTriples = new ArrayBuffer[Tuple4[java.lang.String, java.lang.String, java.lang.String, java.lang.String]]
    var insTriples = new ArrayBuffer[Tuple4[java.lang.String, java.lang.String, java.lang.String, java.lang.String]]

    val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]

    val deleteQuads = updateMod.getDeleteQuads
    val insertQuads = updateMod.getInsertQuads

    for (i <- 0 until deleteQuads.size) {

      delTriples += Tuple4(deleteQuads.get(i).getSubject.toString, deleteQuads.get(i).getPredicate.toString, deleteQuads.get(i).getObject.toString, "DELETE")

    }

    for (i <- 0 until insertQuads.size) {

      insTriples += Tuple4(insertQuads.get(i).getSubject.toString, insertQuads.get(i).getPredicate.toString, insertQuads.get(i).getObject.toString, "INSERT")

    }

    new Tuple2(delTriples, insTriples)

  }

  def applyUpdateSemantics(update: UpdateRequest, semantics: String) = {

    var res: UpdateRequest = null

    if (semantics == "brave") {
      val bs = new BraveSemantics(null)
      res = bs.rewriteSemMat2Brave(update)
    }
    else if (semantics == "cautious") {
      val cs = new CautiousSemantics(null)
      res = cs.rewriteSemMat2Cautious(update)
    }
    else if (semantics == "fainthearted") {
      val fh = new FaintHeartedSemantics(null)
      res = fh.rewriteSemMat2FaintHearted(update)
    }

    res

  }
}

    object TestGroundTriplesFromUpdate {

      def main(args: Array[String]): Unit = {

        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

        val test = new InfoboxSandboxCustom(null, "")
        val ans = test.getGroundTriplesFromUpdate(update)

        for (del <- ans._1) println(del)
        for (ins <- ans._2) println(ins)

      }

    }

    object TestLocalConsistencyCheck {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = new File("./data/downloads/Santi_Cazorla/709848090.xml") // no need for this, subject is taken from update
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val choiceDML = Seq(new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "name", newValue = "Santi", operation = "INSERT"),
          new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "playername", newValue = "Santi CZ", operation = "INSERT"),
          new WikiDML("http://en.wikipedia.org/wiki/Thierry_Henry", "infobox football biography", "playername", newValue = "Thierry", operation = "INSERT"))

        //new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "managerYears", newValue="Arsenal", operation="INSERT"))

        val b = test.checkConsistency(choiceDML, null)

        if (b)
          println("The update: " + choiceDML + " is consistent")
        else
          println("The update: " + choiceDML + " is not consistent")

      }
    }

    object TestGetMappingFromSubject {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null // no need for this, subject is taken from update
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val title = "Santi_Cazorla"

        test.getMappingType(title)

      }

    }


    object TestConsistencyCheck {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = new File("./data/downloads/Thierry_Henry/test") // no need for this, subject is taken from update
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

        val b = test.checkConsistency(update)

        if (b)
          println("The update: " + update + " is consistent")
        else
          println("The update: " + update + " is not consistent")

      }

    }

    object TestInfoboxCount {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = "_ambig.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val titles = Seq("Santi_Cazorla", "Thierry_Henry")

        val infoboxProperties =

          Seq(new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "name",
            newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "fullyname",
              newValue = "Dennis", operation = "INSERT"))

        println(test.countInfoboxProperties(titles, infoboxProperties))
      }

    }

    object TestInfoboxCountFootballPlayersStats {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = "_ambig.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val filePath = "/ISWC/english-players.xml"
        val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

        val infoboxProperties =

          Seq(new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "name",
            newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "fullname",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "playername",
              newValue = "Dennis", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "dateofbirth",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "birth_date",
              newValue = "Dennis", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "cityofbirth",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "countryofbirth",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "birth_place",
              newValue = "Dennis", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "dateofdeath",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "death_date",
              newValue = "Dennis", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "cityofdeath",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "countryofdeath",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "death_place",
              newValue = "Dennis", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs1",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs2",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "youthclubs3",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs1",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs2",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs3",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs4",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs5",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs6",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs7",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs8",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs9",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "clubs10",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam1",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam2",
              newValue = "Dennis", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Dennis_Bergkamp", "Infobox football biography", "nationalteam3",
              newValue = "Dennis", operation = "INSERT")
          )

        println(test.countInfoboxProperties(filePath, infoboxProperties, title))
      }

    }

    object TestInfoboxCountClubsStats {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = "_ambig.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val filePath = "/ISWC/english-teams.xml"
        val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

        val infoboxProperties =
          Seq(new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "clubname",
            newValue = "Arsenal", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "fullname",
              newValue = "Arsenal", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/Arsenal", "Infobox football club", "shortname",
              newValue = "Arsenal", operation = "INSERT"))

        println(test.countInfoboxProperties(filePath, infoboxProperties, title))
      }

    }


    object TestInfoboxCountCitiesStats {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = "_ambig.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val filePath = "/ISWC/english-settlements.xml"
        val title = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."))

        val infoboxProperties =
          Seq(new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "name",
            newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "official_name",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "native_name",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin1",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin2",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin3",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin4",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin5",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin6",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin7",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin8",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin9",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin1_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin2_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin3_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin4_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin5_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin6_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin7_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin8_country",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "twin9_country",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title1",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title2",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title3",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_title4",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name1",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name2",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name3",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "leader_name4",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date1",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date2",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "established_date3",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "population_as_of",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "pop_est_as_of",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "population_total",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "pop_est",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "postal_code",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "postal2_code",
              newValue = "London", operation = "INSERT"),

            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name1",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name2",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name3",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name4",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name5",
              newValue = "London", operation = "INSERT"),
            new WikiDML("http://en.wikipedia.org/wiki/London", "Infobox settlement", "subdivision_name6",
              newValue = "London", operation = "INSERT"))


        println(test.countInfoboxProperties(filePath, infoboxProperties, title))
      }

    }

    object TestResourcesWithSimilarProperties {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = "_ambig.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))

        val query = test.getQueryForResourcesWithSamePredicates(update, 100, null, null) // fixme

        println(query)
      }

    }

    object TestInfoboxPropertyGaps {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = ".xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia04.ru"))

        val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]
        val insertQuads = updateMod.getInsertQuads

        val classType:String = "<http://dbpedia.org/ontology/Settlement>"
//        val classType:String = "<http://dbpedia.org/ontology/SoccerPlayer>"
        //    val classType:String = "<http://dbpedia.org/ontology/Band>"
        //    val classType:String = "<http://dbpedia.org/ontology/AdministrativeRegion>"
        //    val classType:String = "<http://dbpedia.org/ontology/River>"
        //    val classType:String = "<http://dbpedia.org/ontology/SoccerClub>"
        //    val classType:String = "<http://dbpedia.org/ontology/University>"
        //    val classType:String = "<http://dbpedia.org/ontology/BasketballPlayer>"
        //      val classType:String = "<http://dbpedia.org/ontology/Actor>"
        //     val classType:String = "<http://dbpedia.org/ontology/Skier>"
//        val classType: String = "<http://dbpedia.org/ontology/Film>"

        val queries = test.getQueryForResourcesWithSamePredicates(update, 100, insertQuads.get(0).getPredicate.toString(), classType)
        println(queries)

        val subjects = new ArrayBuffer[String]()

        for (query <- queries) {

          val rs = test.getQueryResultsFromDBpedia(query)

          while (rs.hasNext()) {

            val sol = rs.nextSolution.get("?Y").toString
            //        println(sol)
            val title = sol.substring(sol.lastIndexOf("/") + 1)
            if (!subjects.contains(title))
              subjects += title
            // download the pages
          }
        }

        val wikiDMLs = test.resolveUpdate(update)

        val newInfoboxProperties = new ArrayBuffer[WikiDML]()

        // flatten wikiDMLs
        val setWikiDML = wikiDMLs._1.toSeq.flatten.flatten

        for (infobox <- setWikiDML) {

          if (infobox.property.startsWith("twin") && !infobox.property.equals("twins"))
            newInfoboxProperties += infobox
        }

        test.countInfoboxPropertiesWithGaps(subjects, newInfoboxProperties)

      }

    }

    object TestStatistics {

      def main(args: Array[String]): Unit = {

        val testDataRootDir = null
        val mappingFileSuffix = ".xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        // defaults
        var updateStr = "./data/updates/dbpedia04.ru"
        var classType = "<http://dbpedia.org/ontology/SoccerPlayer>"
        var sampleSize = 100

        if (args.length == 3)
        {
          updateStr = args(0)

          if (args(1).equals("SoccerPlayer"))
            classType = "<http://dbpedia.org/ontology/SoccerPlayer>"
          else if (args(1).equals("Film"))
            classType = "<http://dbpedia.org/ontology/Film>"
          else if (args(1).equals("University"))
            classType = "<http://dbpedia.org/ontology/University>"
          else if (args(1).equals("Settlement"))
            classType = "<http://dbpedia.org/ontology/Settlement>"

          sampleSize = args(2).toInt
        }


        val update = UpdateFactory.create(UtilFunctions.readFile(updateStr))

        val updateMod: UpdateModify = update.getOperations.get(0).asInstanceOf[UpdateModify]
        val insertQuads = updateMod.getInsertQuads

        val queries = test.getQueryForResourcesWithSamePredicates(update, sampleSize,
                                          insertQuads.get(0).getPredicate.toString(), classType)

        val subjects = new ArrayBuffer[String]()

        for (query <- queries) {

          val rs = test.getQueryResultsFromDBpedia(query)

          while (rs.hasNext()) {

            val sol = rs.nextSolution.get("?Y").toString
            //        println(sol)
            val title = sol.substring(sol.lastIndexOf("/") + 1)
            if (!subjects.contains(title))
              subjects += title
            // download the pages
          }
        }

        val wikiDMLs = test.resolveUpdate(update)

        // flatten wikiDMLs
        val setWikiDML = wikiDMLs._1.toSeq.flatten.flatten

        println(test.countInfoboxProperties(subjects, setWikiDML))

      }

    }

    object TestUpdate {

      def main(args: Array[String]): Unit = {

        //  Start with no infobox and general mapping (no prefix)
        val testDataRootDir = null
        val mappingFileSuffix = ".xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        //  Instantiate General update to DBpedia and Group it by Subject
        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01.ru"))
        val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

        var atomicUpdate: UpdateRequest = null
        if (updateMod.getWherePattern.toString.contains("?")) {
          // general update contains one variable at least
          atomicUpdate = test.instantiateGeneralUpdate(update)
        }
        else
          atomicUpdate = update

        println(test.resolveUpdate(atomicUpdate))

      }

    }

    object TestUpdateSemantics {

      def main(args: Array[String]): Unit = {

        // infobox
        val testDataRootDir = new File("./data/downloads/Santi_Cazorla/703998546.xml")

        // mappings
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        // update
        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01a.ru"))

        val rewUpdate = test.applyUpdateSemantics(update, "brave")

        println(rewUpdate)
      }

    }

    object TestInsertUpdate {

      def main(args: Array[String]): Unit = {

        // infobox
        val testDataRootDir = new File("./data/downloads/Santi_Cazorla/703998546.xml")

        // mappings
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        // update
        val update = UpdateFactory.create(UtilFunctions.readFile("./data/updates/dbpedia01a.ru"))

        val ins = test.getGroundTriplesFromUpdate(update)._2

        test.update = ins(0)

        val res = test.resolveForLanguage(testDataRootDir, Language.English)

        println(res)

      }

    }


    object TestDiffFromInfoboxUpdate {

      def main(args: Array[String]): Unit = {

        // infobox
        val testDataRootDir = new File("./data/downloads/Santi_Cazorla/709848090.xml")

        // mappings
        val mappingFileSuffix = "_ambig.xml"

        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        // inserts
        val choiceDML = Seq(new WikiDML("http://en.wikipedia.org/wiki/Santi_Cazorla", "infobox football biography", "name", newValue = "Santi", operation = "INSERT"),
          new WikiDML("Santi_Cazorla", "infobox football biography", "playername", newValue = "Santi CZ", operation = "INSERT"))

        println(test.getDiffFromInfoboxUpdate(choiceDML))

      }
    }

    // use this for extracting a page
    object TestInfoboxSandboxCustomMappings {

      def main(args: Array[String]): Unit = {

        // Downloaded Infobox-es
        val testDataRootDir = new File("./data/downloads/test/Broward_County_Library.xml")

        // Mappings suffix
        val mappingFileSuffix = "_test.xml"
        val test = new InfoboxSandboxCustom(testDataRootDir, mappingFileSuffix)

        println(test.renderForLanguage(testDataRootDir, Language.English))

      }
}