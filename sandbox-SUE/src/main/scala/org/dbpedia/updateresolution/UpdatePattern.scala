package org.dbpedia.updateresolution

import org.dbpedia.extraction.destinations.Quad

import scala.collection.mutable.Buffer
import scala.collection.mutable.ArrayBuffer

class UpdatePattern (
        val infoboxType: String, // should be URI, what is a relevant datatype
        val wikiDelete: Buffer[Quad] = ArrayBuffer[Quad](), // each triple represents a deleted Wiki property
        val wikiInsert: Buffer[Quad] = ArrayBuffer[Quad](), // each triple represents an inserted Wiki property using variables
        val rdfDelete: Buffer[Quad] = ArrayBuffer[Quad](), // deleted dbpedia triples
        val rdfInsert: Buffer[Quad] = ArrayBuffer[Quad](), // inserted dbpedia triples using variable
        val origin: String = UpdatePattern.RDF_INSERT,
        val inducedReason: InducedUpdateReason=null,
        val inducedBy: UpdatePattern=null)
  {
  override def toString(): String = toString(x=>x)

  def iso(propMap:String=>String, source:String):Unit = {
    val (from,to,newctx) =
    source match {
      case UpdatePattern.RDF_INSERT => (rdfInsert,wikiInsert,UpdatePattern.WIKI_INSERT)
      case UpdatePattern.WIKI_INSERT => (wikiInsert,rdfInsert,UpdatePattern.RDF_INSERT)
      case UpdatePattern.RDF_DELETE => (rdfDelete,wikiDelete,UpdatePattern.WIKI_DELETE)
      case UpdatePattern.WIKI_DELETE => (wikiDelete,rdfDelete,UpdatePattern.RDF_DELETE)
    }

    to.appendAll(
      if( to.isEmpty )
        from.foldLeft(Seq.empty[Quad])((qs, quad)=>qs :+ quad.copy(predicate=propMap(quad.predicate),context=newctx) )
      else Seq.empty[Quad]
    )
  }

  def toString( f: String => String ): String = {
    val prefix = s"InfoboxTemplate($infoboxType)"

    val delOp = if (wikiDelete.isEmpty) ""
                else s"DELETE " + wikiDelete.map(d =>s"$prefix.${d.predicate}").mkString(", ")
    val insOp = if (wikiInsert.isEmpty) ""
                else "INSERT " + wikiInsert.map(d =>
                    s"$prefix.${d.predicate} = ${f(d.value)}").mkString(", ")

    s"ON wikiPage = ${f("?PAGE")} $delOp $insOp"
  }

  def exportString(f: String => String): String = {
    val S = "$$"
    val d = wikiDelete.map(d =>
      s"${f("?PAGE")}${S}${infoboxType}${S}${d.predicate}$S ${S}DELETE${S}")
    val i = wikiInsert.map(d =>
      s"${f("?PAGE")}${S}${infoboxType}${S}${d.predicate}$S ${S}UPDATE${S}")

    d.mkString("\n") + "\n" + i.mkString("\n")
  }

}

object UpdatePattern {
  val LANGUAGE = "en"
  val STRING = "xsd:string"
  val PAGE = "?PAGE"

  var lastVarName = "?@" // init with a char preceding A

  var WIKISIDE_UPDAE = "wiki"
  var RDFSIDE_UPDATE = "rdf"

  val WIKI_INSERT = "WikiInsert"
  val WIKI_DELETE = "WikiDelete"

  val RDF_INSERT = "RDFInsert"
  val RDF_DELETE = "RDFDelete"

  def wd(property:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, freshVar, WIKI_DELETE, STRING)
    new UpdatePattern(PAGE, wikiDelete=ArrayBuffer(q))
  }
  def wd(property:String, value:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, value, WIKI_DELETE, STRING)
    new UpdatePattern(PAGE, wikiDelete=ArrayBuffer(q))
  }

  def rd(property:String) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, freshVar, RDF_DELETE, STRING)
    new UpdatePattern(PAGE, rdfDelete=ArrayBuffer(q))
  }
  def rd(property:String, objct: String) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, objct, RDF_DELETE, STRING)
    new UpdatePattern(PAGE, rdfDelete = ArrayBuffer(q))
  }

  def wi(property:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, freshVar, WIKI_INSERT, STRING)
    new UpdatePattern(PAGE, wikiInsert=ArrayBuffer(q))
  }
  def wi(property:String, value:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, value, WIKI_INSERT, STRING)
    new UpdatePattern(PAGE, wikiInsert=ArrayBuffer(q))
  }

  def ri(property:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, freshVar, RDF_INSERT, STRING)
    new UpdatePattern(PAGE, rdfInsert=ArrayBuffer(q))
  }
  def ri(property:String, objct:String ) : UpdatePattern = {
    val q = new Quad(LANGUAGE, null, PAGE, property, objct, RDF_INSERT, STRING)
    new UpdatePattern(PAGE, rdfInsert=ArrayBuffer(q))
  }

  def toRdf( wikiQuad:Quad, rdfProperty:String ) = wikiQuad.copy(context = rdfContext(wikiQuad.context), predicate = rdfProperty)
  def toWiki( rdfQuad:Quad, wikiProperty:String ) = rdfQuad.copy(context = wikiContext(rdfQuad.context), predicate = wikiProperty)

  def freshVar():String = {
    val l = (lastVarName.last + 1).toChar.toString
    lastVarName = (if(l >= "Z") lastVarName + "A" else lastVarName.dropRight(1)) + l
    lastVarName
  }
  def rdfContext(context : String):String = {
    context match {
      case RDF_INSERT | WIKI_INSERT => RDF_INSERT
      case RDF_DELETE | WIKI_DELETE => RDF_DELETE
      case _ => null
    }
  }
  def wikiContext(context : String):String = {
    context match {
      case RDF_INSERT | WIKI_INSERT => WIKI_INSERT
      case RDF_DELETE | WIKI_DELETE => WIKI_DELETE
      case _ => null
    }
  }
}
