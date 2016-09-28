package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{WikiPage, FileSource, XMLSource}
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.ontology.io.OntologyReader
import io.Source
import org.dbpedia.extraction.util.Language
import java.io.{FilenameFilter, File}
import java.lang.IllegalStateException
import scala.collection.mutable.ArrayBuffer
//import org.junit.{Ignore, Test}
import org.dbpedia.extraction.ontology.Ontology
import scala.language.reflectiveCalls


class InfoboxSandboxDirect
{
  private val testDataRootDir = new File("./data/downloads/Thierry_Henry/test" +
    "")

  private val filter = new FilenameFilter
  {
    def accept(dir: File, name: String) = name endsWith ".xml"
  }

  private val formater = new TerseFormatter(true,true)
  private val parser = WikiParser.getInstance()

  def run()
  {
    /*
     * Files in testDataRootDir are assumed to come from DBpedia en
     */
    testForLanguage(testDataRootDir, Language.English)

    /*
     * For linguistic chapters, files are assumed to be in subfolders given by their iso code
     */
    for(langFolder <- testDataRootDir.listFiles.filter(a => a.isDirectory())){
      Language.get(langFolder.getName) match {
        case Some(l) => testForLanguage(langFolder, l)
        case None =>
      }
    }

    //val lang = Language.getOrElse("fr", Language.English)
  }

  def testForLanguage(folder : File, _language : Language){
    println("Exploring the folder " + folder.getAbsolutePath())

    val context = new {

      def ontology = {
        val ontoFilePath = "ontology.xml"
        val ontoFile = new File(ontoFilePath)
        val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
        new OntologyReader().read(ontologySource)
      }
      def language = _language
      def redirects = new Redirects(Map())

    }

    for(f <- folder.listFiles(filter) )
    {
      println("test file " + f.getName())

      test(f.getName, context, folder)
    }
  }

  def test(fileNameWiki : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder:File)
  {
    println("testing wiki "+fileNameWiki)

    val d = render(fileNameWiki, context, folder).map(formater.render(_).trim()).toSet

    d.foreach(println)

  }

  private def render(file : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects}, folder : File) : Seq[Quad] =
  {
    val extractor = new InfoboxExtractor(context)

    println("input file : " + folder + "/" + file)
    val page = //new FileSource(folder, context.language, _ endsWith file).head
      XMLSource.fromFile(new File(folder.getPath() + "/" + file),context.language).head
    println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(n) => extractor.extract(n,page.title.resourceIri,new PageContext())
      case None => Seq.empty

    }

  }


}

object TestDirectMappings {
  def main(args: Array[String]): Unit = {
    val test = new InfoboxSandboxDirect
    test.run()
  }
}