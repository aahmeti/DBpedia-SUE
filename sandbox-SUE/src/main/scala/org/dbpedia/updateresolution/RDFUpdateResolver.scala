package org.dbpedia.updateresolution

import java.io._
import java.util
import java.nio.charset.StandardCharsets
import javax.xml.stream.XMLStreamException

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.sparql.core
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
import org.dbpedia.extraction.{ExtractLogic, WikiDML}

import scala.collection.mutable.{ArrayBuffer, HashMap, Set, MultiMap}
import scalax.file.Path
import at.tuwien.dbai.rewriter._
import at.tuwien.dbai.rewriter.incons._
import org.dbpedia.extraction.ontology.Ontology

import scala.language.reflectiveCalls



class RDFUpdateResolver(   var testDataRootDir:File
                            , var mappingFileSuffix:String
                            , downloadDirectory: String="./data/downloads"
                            , ontologyName: String= "ontology.xml"
                            , mapping_Path: String= "mappings")
{
  var update: (String,String,String,String) = null
  val chosenInfoboxUpdate = null
  private var download_directory = downloadDirectory
  private var ontologyPath = ontologyName
  private var mappingsPath = mapping_Path
  private val placeholder = "$$$"
  private val parser = WikiParser.getInstance()

  val WIKI_API_URL_PREFIX = "https://en.wikipedia.org/w/api.php?" +
  "action=query&prop=revisions&format=xml&rvprop=ids|timestamp|userid|sha1|content&rvlimit=1&rvgeneratexml=&rvcontentformat=text%2Fx-wiki&rvstart=now&rvdir=older&exportnowrap=&titles="

  val ontoFilePath = ontologyPath
  println(ontoFilePath)
  val ontoFile = new File(ontoFilePath)
  val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
  val ontoObj = new OntologyReader().read(ontologySource)

  private val context = new {

    val ontology = ontoObj

    val language = Language.English

    val mappingPageSource = {
      val namespace = Namespace.mappings(language)

      val file = new File(mappingsPath,
        namespace.name(Language.Mappings).replace(' ', '_') +
          mappingFileSuffix)

      XMLSource.fromFile(file, Language.Mappings)

    }

    val redirects = new Redirects(Map())
  }

  //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
  // Loading mappings and redirects
  //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

  private val contextMappings = new {
   // println("context create!")
    val mappings: Mappings = MappingsLoader.load(context)
    val templateMappings = mappings.templateMappings
    val redirects: Redirects = new Redirects(Map())
  }

  private val extractor = new MappingExtractor(contextMappings)

  def setConfiguration(downloadDirectory: String, ontologyName: String, mapping_Path: String) {
    download_directory = downloadDirectory
    ontologyPath = ontologyName
    mappingsPath = mapping_Path
  }

  def setUpdate(tp: (String,String,String,String)) {
    update = tp
  }


  def checkConsistencyFromUpdate(updateQuery: String) = {
    val update = UpdateFactory.create(updateQuery)
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
    val page = XMLSource.fromFile(new File(folder.getPath()), context.language).head
    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(node) =>
          extractor.extractWithInfoboxChanges(node, page.title.resourceIri, new PageContext(), chosenInfoboxUpdate)
      case None => Seq.empty
    }
  }

  /**
   * Main entry function: Renders a wiki page to a set of Quads
    *
    * @param file wiki page
   * @param _language language of the mappings
   * @return Set of Quads
   */
  def renderForLanguage(file: File, _language: Language): Seq[Quad] = {
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
    render(file.getName, context, file)
  }

  /**
   * Renders a wiki page to a set of Quads
    *
    * @param file the wiki page
   * @param context the mappings and ontology
   * @param folder folder of the wiki page
   * @return
   */
  private def render(  file: String
                     , context: AnyRef {def ontology: Ontology; def language: Language; def redirects: Redirects; def mappingPageSource: Traversable[WikiPage]}
                     , folder: File): Seq[Quad] = {

    val page = XMLSource.fromFile(new File(folder.getPath()), context.language).head
    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(node) => extractor.extract(node, page.title.resourceIri, new PageContext())
      case None => Seq.empty
    }
  }

  def countInfoboxPropertiesWithGaps(titles: Seq[String], infoboxProperties: Seq[WikiDML]) = {

    val infoboxTitle = infoboxProperties(0).infobox.toUpperCase()
    val gaps = new Array[Int](infoboxProperties.size)


    for (title <- titles) {
      println(title)
      // TODO: check if title exists in the specified path

      // get all properties from titles
      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val apiUrl = WIKI_API_URL_PREFIX + title

      val extractLogic = new ExtractLogic

      try {
        if (!new File(s"$download_directory/$title/page.xml").exists()) {
          extractLogic.downloadAndCreateTemplate2(pathName, title, apiUrl)
        }

        val page = XMLSource.fromFile(new File(s"$download_directory/$title/page.xml"), Language.English).head

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
          case None =>
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
        case _: XMLStreamException => println("XML page could not be read:" + pathName)
      }

    }
    gaps

  }

  def countInfoboxPropertiesFromSubjectsProperty(titles: Seq[String], property:String) = {

    val infoboxTitles = new ArrayBuffer[String] // used for filtering infoboxes based on titles by wikiDML
    val infoboxCount = new HashMap[String, Int]

    for (title <- titles) {

      // get all properties from titles
      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val apiUrl = WIKI_API_URL_PREFIX + title

      val extractLogic = new ExtractLogic

      try {

        if (!new File(download_directory + "/" + title + "/page.xml").exists()) {
          val (revID, template) = extractLogic.downloadAndCreateTemplate2(pathName, title, apiUrl)
        }

        val page = XMLSource.fromFile(new File(download_directory + "/" + title + "/page.xml"), Language.English).head

        parser(page) match {
          case Some(node) =>

            val infoboxes = collectTemplates(node)
            for (infobox <- infoboxes) {
              for (k <- infobox.keySet) {
                for (p<- infobox.property(k)) {
                 println(p.toWikiText)
                  if (p.toWikiText.contains(property)) {
                    infoboxCount(k) += 1
                  }
                }

              }
            }
          case None =>
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
        case _: XMLStreamException => println("XML page could not be read:" + pathName)
      }
    }

    infoboxCount
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

    for (title <- titles) {
      // get all properties from titles
      val pathName = download_directory + "/" + title
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val apiUrl = WIKI_API_URL_PREFIX + title

      val extractLogic = new ExtractLogic
      val filePath = s"$download_directory/$title/page.xml"

      try {

        if (!new File(filePath).exists()) {
          extractLogic.downloadAndCreateTemplate2(pathName, title, apiUrl)
        }

        val page = XMLSource.fromFile(new File(filePath), Language.English).head

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
          case None =>
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
        case _: XMLStreamException => println("XML page could not be read:" + pathName)
        case _: IllegalArgumentException => println("IllegalArgumentException!:" + pathName)
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
          case None =>
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

  def resolveUpdate(update: UpdateRequest, computeStat:Boolean=false): util.Map[String,java.util.Set[Update]] = {

    val result = new HashMap[String,util.Set[Update]]

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Loading a page from a XML file
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    val bySubj = RDFUpdateResolver.groupBySubject(update)
    val extractLogic = new ExtractLogic

    for ((subj, u) <- bySubj) {

      //partial translations
      val pt = new HashMap[Quad,Set[UpdatePattern]] with MultiMap[Quad,UpdatePattern]

      var pageTitle = subj
      if (pageTitle.indexOf("/") != -1)
        pageTitle = pageTitle.substring(pageTitle.lastIndexOf("/") + 1)

      val pathName = download_directory + "/" + pageTitle
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val apiUrl = WIKI_API_URL_PREFIX + pageTitle

      try {
        val (revID, _) = extractLogic.downloadAndCreateTemplate(pathName, pageTitle, apiUrl)

        val page = XMLSource.fromFile(new File(s"$download_directory/$pageTitle/$revID.xml"), Language.English).head

        parser(page) match {
          case Some(node) =>
            for (delp <- u.pattern.rdfDelete) {
               extractor.resolve( node
                 , subjectUri = subj
                 , context = new PageContext()
                 , updateSubjectUri = null
                 , delp.predicate
                 , delp.value
                 , "DELETE").foreach( x => pt.addBinding(delp,UpdatePattern.fromWikiDML(x)) )
            }

            u.pattern.rdfInsert.foreach(ins => {
              resolveInsert( subj, node, ins ).foreach( x => pt.addBinding(ins,UpdatePattern.fromWikiDMLs(x)) )
            })

          case None =>
        }
      }
      catch {
        case _: NoSuchElementException => println("Bad path name: " + pathName)
      }

      val parts = UpdatePattern.product(pt.map( {case (k,ups) => ups})).map(x => UpdatePattern.merge(x))
      result(subj) = parts.map( x => new Update(x, u.instantiation) ).toSet.asJava
    }
    result.asJava
  }


  def resolveInsert(subject:String, node:PageNode, insAtom : Quad) : Seq[Seq[WikiDML]] = {

    val dmls = new ArrayBuffer[Seq[WikiDML]]()

    val relevantNames = collectTemplateNames(node).toSet

    val pred = insAtom.predicate
    val value = insAtom.value

    for ((templTitle, m) <- contextMappings.templateMappings.filterKeys(relevantNames)) {
      m match {
        case condm: ConditionalMapping => {

          for (i <- 0 until condm.cases.size) {

            val condCase = condm.cases(i)

            for (propertyMapping <- condCase.mapping.asInstanceOf[TemplateMapping].mappings) {

              if (propertyMapping.isInstanceOf[ConstantMapping]) {

                if ((propertyMapping.asInstanceOf[ConstantMapping].ontologyProperty.uri == pred)
                  && (propertyMapping.asInstanceOf[ConstantMapping].value == value)) {

                  for ( template <- collectTemplates(node) ) {
                    val resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase

                    if (templTitle.toUpperCase == resolvedTitle.toUpperCase) {

                      // TODO: other conditions here
                      val condDML = new ArrayBuffer[WikiDML]
                      if (condCase.operator == "isSet") {

                        condDML += new WikiDML(node.title.decoded, resolvedTitle, condCase.templateProperty, newValue = placeholder, operation = "INSERT")

                        for (j <- 0 until i) {
                          val condDisabled = condm.cases(j)
                          condDML += new WikiDML(node.title.decoded, resolvedTitle, condDisabled.templateProperty, operation = "DELETE")
                        }
                      }
                      dmls += condDML.toList
                    }
                  }
                }
              }
              // end of "if" constant mapping
            }
            // end of "for" all mappings in conditional case
          }

          for (mapping <- condm.defaultMappings) {
            if (mapping.isInstanceOf[SimplePropertyMapping]) {
              val templateProperty = mapping.asInstanceOf[SimplePropertyMapping].templateProperty
              val ontologyProperty = mapping.asInstanceOf[SimplePropertyMapping].ontologyProperty

              // println(templateProperty + " --> " + ontologyProperty)

              if (ontologyProperty.uri == pred) {

                //               println(collectTemplates(n).size)
                for (template <- collectTemplates(node)) {
                  val resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase

                  if (templTitle.toUpperCase == resolvedTitle.toUpperCase)
                    dmls += Seq(new WikiDML(node.title.decoded, resolvedTitle, templateProperty, newValue = value, operation = "INSERT"))
                }

              }
            }
          }

        }
        // case conditional mapping

        case tm: TemplateMapping => {

          for (mapping <- tm.mappings) {

            if (mapping.isInstanceOf[SimplePropertyMapping]) {

              // TODO: ConstantMapping?
              val templateProperty = mapping.asInstanceOf[SimplePropertyMapping].templateProperty
              val ontologyProperty = mapping.asInstanceOf[SimplePropertyMapping].ontologyProperty

              if (ontologyProperty.uri == pred) {

                for (template <- collectTemplates(node)) {
                  val resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
                  //println(templateMapping._1 + " == " + resolvedTitle)
                  if (templTitle.toUpperCase == resolvedTitle.toUpperCase)
                    dmls += Seq(new WikiDML(node.title.decoded, resolvedTitle, templateProperty, newValue = value, operation = "INSERT"))
                }

              }
            }
          }
        }

      } //match m

    }

    dmls
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

  private def collectTemplateNames(node: Node): Seq[String] = {
    collectTemplates(node).map(t => context.redirects.resolve(t.title).decoded.toLowerCase)
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

    (del, ins)
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
      var pageTitle = update(0).wikiPage

      if (pageTitle.indexOf("/") != -1)
        pageTitle = pageTitle.substring(pageTitle.lastIndexOf("/") + 1)

      val quads = extractQuads(pageTitle) // extract the quads from title

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

      val dbpediaPath = download_directory + "/" + "dbpedia_2014.owl"

      val dataset = TDBFactory.createDataset()
      dataset.begin(ReadWrite.WRITE)
      val model = dataset.getDefaultModel()
      val ts = new TripleStore(model)

      ts.init(dbpediaPath, "RDF/XML")

      val sb = new StringBuilder()

      for (quad <- result) {
         sb.append( t.render(quad) )
      }

      model.read( new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), null, "N3" )

      ts.materializeDBpedia()

      ts.setDataset(dataset)

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
      val rs = ts.getResultsFromQuery(queryConsistencyCheck);

      if (rs.hasNext) {
        while (rs.hasNext()) {
          rs.next()
        }
        ts.getDataset().end()

        bConsistent = false
      }
      ts.getDataset().end()
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

    val apiUrl = WIKI_API_URL_PREFIX + sTitle

    val extractLogic = new ExtractLogic

    try {
      val (revID, template) = extractLogic.downloadAndCreateTemplate(pathName, sTitle, apiUrl)

      val page = XMLSource.fromFile(new File(s"$download_directory/$sTitle/$revID.xml"), Language.English).head

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

    val sTitle = if (title.indexOf("/") != -1) {
      title.substring(title.lastIndexOf("/") + 1)
    }
    else { title }

    val folder = new File(download_directory)

    val pathName = folder.getPath() + "/" + sTitle
    val newPath: Path = Path.fromString(pathName)
    newPath.createDirectory(failIfExists = false)

    val apiUrl = WIKI_API_URL_PREFIX + sTitle

    val extractLogic = new ExtractLogic

    try {
      val (revID,_) = extractLogic.downloadAndCreateTemplate(pathName, sTitle, apiUrl)
      val page = XMLSource.fromFile(new File(s"$download_directory/$sTitle/$revID.xml"), Language.English).head
      val subjectUri = "http://dbpedia.org/page/" + sTitle

      parser(page) match {
        case Some(n) => extractor.extract(n, subjectUri, new PageContext())
        case None => Seq.empty
      }
    }
    catch {
      case ex: Throwable => {
        println(s"Bad path name $pathName? ${ex.getMessage}")
        Seq.empty
      }
    }
  }


  /**
   * Checks if SPARQL update is consistent or not
    *
    * @param update
   * @return true if update is consistent
   */

  def checkConsistency(update: UpdateRequest): Boolean = {

    val bySubj = RDFUpdateResolver.groupBySubject(update)
    val folder = new File(download_directory)
    val subjQuads = new ArrayBuffer[Quad]()
    val extractLogic = new ExtractLogic

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Extract triples foreach subject
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    for ((subj, upd) <- bySubj.filter({case(_,u)=> !u.pattern.wikiInsert.isEmpty})) {

      var pageTitle = subj

      if (pageTitle.indexOf("/") != -1)
        pageTitle = pageTitle.substring(pageTitle.lastIndexOf("/") + 1)

      val pathName = folder.getPath() + "/" + pageTitle
      val newPath: Path = Path.fromString(pathName)
      newPath.createDirectory(failIfExists = false)

      val apiUrl = WIKI_API_URL_PREFIX + pageTitle

      try {
        val (revID,_) = extractLogic.downloadAndCreateTemplate(pathName, pageTitle, apiUrl)

        val page = XMLSource.fromFile(new File(s"$download_directory/$pageTitle/$revID.xml"), Language.English).head

        subjQuads ++= { parser(page) match {
                             case Some(n) => extractor.extract(n, subj, new PageContext())
                             case None => Seq.empty }
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
    val pathname = download_directory + "/" + "context.ttl"
    val file = new File(pathname);
    file.delete()
    for (quad <- subjQuads) {
      extractLogic.printToFile2(pathname, t.render(quad))
    }

    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Perform the update
    //  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    val dbpediaPath = s"$download_directory/dbpedia_2014.owl"
    val tdbPath = s"$download_directory/TDB"
    val dataset = TDBFactory.createDataset(tdbPath)
    dataset.begin(ReadWrite.WRITE)
    val model = dataset.getDefaultModel
    val ts = new TripleStore(model)
    ts.setDataset(dataset)

    ts.clear
    ts.init(pathname, "N3")
    ts.init(dbpediaPath, "RDF/XML")

    ts.runUpdate(update)
    ts.materialize

    ts.runUpdate(update)
    ts.materializeDBpedia
    ts.getDataset.getDefaultModel.commit
    ts.getDataset.getDefaultModel.close
    ts.getDataset.end
    ts.getDataset.close

    val ts2 = new TripleStore(tdbPath, true)

    val queryConsistencyCheck = """
      SELECT ?subject
      | WHERE { {
      |  ?subject a ?type .
      |  ?subject a ?type2 .
      |  ?type <http://www.w3.org/2002/07/owl#disjointWith> ?type2 . }
      |  UNION { ?subject ?predicate ?object .
      |  ?subject ?predicate2 ?object .
      |  ?predicate <http://www.w3.org/2002/07/owl#propertyDisjointWith> ?predicate2 }} """
      .stripMargin

    // todo: return the inconsistent triple
    val rs = ts2.getResultsFromQuery(queryConsistencyCheck)
    val result = !rs.hasNext

    ts2.getDataset.end

    result
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

    val apiUrl = WIKI_API_URL_PREFIX + title

    val extractLogic = new ExtractLogic

    try {
      val (revID, _) = extractLogic.downloadAndCreateTemplate(pathName, title, apiUrl)

      val page = XMLSource.fromFile(new File(s"$download_directory/$title/$revID.xml"), Language.English).head

      parser(page) match {
        case Some(node) =>
          for (template <- collectTemplates(node)){
            var resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
          }
        case None =>
      }
    }
    catch {
      case _: NoSuchElementException => println("Bad path name: " + pathName)
    }

  }

  def getStatResultsAlternatives(subjectDbpedia: String, predicateDbpedia:String, sampleSize:Int,wikiAlternatives:Seq[Seq[WikiDML]]) =
  {

    val subjType = getSubjectType(subjectDbpedia)
   // println(subjType)
    val rs = RDFUpdateResolver.queryDBpedia(subjType.toString)

    val resType = new ArrayBuffer[String]()

    while (rs.hasNext()) {

      val sol = rs.nextSolution.get("?Y").toString
      // filter only those types from dbpedia ontology
      if (sol.contains("http://dbpedia.org/ontology")) {
        resType += sol
      }
    }

    val queries = getQueryForResourcesWithSamePredicates(sampleSize, predicateDbpedia, resType.toSeq)

    println(queries)
    val subjects = new ArrayBuffer[String]()

    for (query <- queries) {
      //println(query)
      val rs = RDFUpdateResolver.queryDBpedia(query)

      while (rs.hasNext()) {

        val sol = rs.nextSolution.get("?Y").toString
       // println("Solution: "+sol)
        val title = sol.substring(sol.lastIndexOf("/") + 1)
        if (!subjects.contains(title))
          subjects += title
        // download the pages
      }
    }

    countInfoboxProperties(subjects, wikiAlternatives.flatten)
  }


  def getSubjectType(subject:String) = {

   // val res = ArrayBuffer[String]()

    val dataset = "SELECT DISTINCT ?Y \n" +
      "WHERE { " + subject + " a ?Y  . }"

    //res += dataset

    //res
    dataset
  }

  def getQueryForResourcesWithSamePredicates(limit: Int, predicate: String, classType: Seq[String]) = {

    //    val groupedAtomicUpdate = groupUpdateBySubject(update)
    //    val inserts = groupedAtomicUpdate._2

    val res = ArrayBuffer[String]()



    var dataset = "SELECT DISTINCT ?Y \n" +
                  "WHERE { "

    for (cType <- classType)
    {
      dataset +=  "?Y a <" + cType + "> . \n"
    }

    dataset += "?Y <" + predicate + "> ?Z2 . \n" +
            "} ORDER BY RAND() LIMIT " + limit

    res += dataset


    res
  }

  def getQueryForResourcesWithSamePredicates(limit: Int, predicate: String, classType: String) = {

//    val groupedAtomicUpdate = groupUpdateBySubject(update)
//    val inserts = groupedAtomicUpdate._2

    val res = ArrayBuffer[String]()

    val dataset = "SELECT DISTINCT ?Y \n" +
      "WHERE { ?Y a " + classType + " . \n" +
      "?Y <" + predicate + "> ?Z2 . \n" +
      "} LIMIT " + limit
    res += dataset

    println(dataset)

    res
  }

  def AskQueryFromDBpedia(queryStr: String):Boolean =
  {
    var res=false
    val query = QueryFactory.create(queryStr)

    val qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)

    try {
      res = qexec.execAsk()
    }
    finally {
      //qexec.close()
    }

    res

  }

  /*
   * Main interface entry point
   *
   *
   */
  def updateFromUpdateQuery(updateQuery: String) = {

    val update = UpdateFactory.create(updateQuery)
    val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

    val atomicUpdate =
      if (updateMod.getWherePattern.toString.contains("?")) {
        // general update contains one variable at least
        RDFUpdateResolver.instantiateGeneralUpdate(update)
      }
      else { update }

    resolveUpdate(atomicUpdate)
  }

  def getGroundTriplesFromUpdateQuery(updateQuery: String) = {
    val update = UpdateFactory.create(updateQuery)
    val (_,ins) = RDFUpdateResolver.getGroundTriplesFromUpdate(update)
    setUpdate(ins(0))
  }

  def applyUpdateSemantics(update: UpdateRequest, semantics: String) = {
    semantics match {
      case "brave" => new BraveSemantics(null).rewriteSemMat2Brave(update)
      case "cautious" => new CautiousSemantics(null).rewriteSemMat2Cautious(update)
      case default => new FaintHeartedSemantics(null).rewriteSemMat2FaintHearted(update)
    }
  }

  def subjectandPredicateExists(triple:String):Boolean={

    val positionFirstspace=triple.indexOf(" ")
    val positionSecondspace=triple.indexOf(" ",positionFirstspace+1)
    val subjPredTriple = triple.substring(0,positionSecondspace)+" ?object"
    val query = "ASK { " + subjPredTriple + "}"

    AskQueryFromDBpedia(query)
  }

  def tripleExists(triple:String) = AskQueryFromDBpedia(s"ASK { $triple }")

}

object RDFUpdateResolver {

  def queryDBpedia(queryStr: String) : ResultSet = {

    val query = QueryFactory.create(queryStr)
    val qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query)

    try {
      qexec.execSelect
    }
    finally {
      qexec.close()
    }
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


  /**
    * Groups a SPARQL update by subject
    *
    * @param update
    * @return a tuple of deletes and inserts
    */
  def groupBySubject(update: UpdateRequest) : Map[String,Update] = {

    val um = update.getOperations.get(0).asInstanceOf[UpdateModify]
    val bySubj = new HashMap[String,(Set[core.Quad],Set[core.Quad])]

    for (dq <- um.getDeleteQuads) {
      val subj = dq.getSubject.toString
      if( !bySubj.contains(subj) ){
        bySubj.put(subj, (Set.empty, Set.empty))
      }
      val (ds,_) = bySubj(dq.getSubject.toString)
      ds.add(dq)
    }

    for (iq <- um.getInsertQuads) {
      val subj = iq.getSubject.toString
      if( !bySubj.contains(subj) ){
        bySubj.put(subj, (Set.empty,Set.empty))
      }
      val (_,is) = bySubj(iq.getSubject.toString)
      is.add(iq)
    }

    //"Map() ++" to get an immutable return object
    Map() ++ bySubj map {
      case (subj,(ds,is)) => (subj, Update.fromSPARQLAtoms(ds.toSeq,is.toSeq))
    }
  }


  /**
    * Transforms an update to a set of tuples of length 4, for both delete and insert
    * (subject, predicate, object, operation)
    *
    * @param update
    * @return a tuple for both delete and insert
    */
  def getGroundTriplesFromUpdate(update: UpdateRequest) = {

    val updateMod = update.getOperations.get(0).asInstanceOf[UpdateModify]

    val delTriples = updateMod.getDeleteQuads.map(
      q =>(q.getSubject.toString,q.getPredicate.toString,q.getObject.toString, "DELETE"))

    val insTriples = updateMod.getInsertQuads.map(
      q =>(q.getSubject.toString,q.getPredicate.toString,q.getObject.toString, "INSERT"))

    (delTriples, insTriples)
  }

}