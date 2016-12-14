package org.dbpedia.updateresolution

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.destinations.Quad

import scala.collection.mutable.{ArrayBuffer, HashSet, Set}

case class UpdatePattern (
        var infoboxType: String = null, // should be URI, what is a relevant datatype
         wikiDelete: Set[Quad] = HashSet[Quad](), // each triple represents a deleted Wiki property
         wikiInsert: Set[Quad] = HashSet[Quad](), // each triple represents an inserted Wiki property using variables
         rdfDelete: Set[Quad] = HashSet[Quad](), // deleted dbpedia triples
         rdfInsert: Set[Quad] = HashSet[Quad](), // inserted dbpedia triples using variable
         origin: String = UpdatePattern.RDF_INSERT,
         inducedReason: InducedUpdateReason=null,
         inducedBy: UpdatePattern=null)
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

    { if (to.isEmpty)
        from.foldLeft(Seq.empty[Quad])((qs, quad) => qs :+ quad.copy(predicate = propMap(quad.predicate), context = newctx))
      else Seq.empty[Quad]
    }.foreach( x => to.add(x) )
  }

  def toString( f: String => String = {x=>x}
                ,  component:String = s"${UpdatePattern.WIKI_DELETE} ${UpdatePattern.WIKI_INSERT}" ): String = {

    val delOp =  if( component.contains(UpdatePattern.WIKI_DELETE) ) {
                    val prefix = if(infoboxType!=null) s"InfoboxTemplate($infoboxType)" else ""
                    if (wikiDelete.isEmpty) ""
                    else s"DELETE " + wikiDelete.map(d => s"$prefix.${d.predicate}").mkString(", ")
                  }
                  else ""

    val insOp =  if( component.contains(UpdatePattern.WIKI_INSERT) ) {
                    val prefix = if(infoboxType!=null) s"InfoboxTemplate($infoboxType)" else ""
                    if (wikiInsert.isEmpty) ""
                    else s"INSERT " + wikiInsert.map(d => s"$prefix.${d.predicate} = ${f(d.value)}").mkString(", ")
                  }
                  else ""

    val wikiPart = if( !(delOp+insOp).isEmpty ) s"ON wikiPage = ${f(UpdatePattern.PAGE)} $delOp $insOp" else ""

    val rdfDelOp =  if( component.contains(UpdatePattern.RDF_DELETE) ) {
                      val prefix = ""
                      if (rdfDelete.isEmpty) ""
                      else s"DELETE " + wikiDelete.map(d => s"$prefix.${d.predicate}").mkString(", ")
                    }
                    else ""

    val rdfInsOp =  if( component.contains(UpdatePattern.RDF_INSERT) ) {
                      val prefix = ""
                      if (rdfInsert.isEmpty) ""
                      else s"INSERT " + wikiInsert.map(d => s"$prefix.${d.predicate} = ${f(d.value)}").mkString(", ")
                    }
                    else ""

    val rdfPart = if( !(rdfDelOp+rdfInsOp).isEmpty ) s"$delOp $insOp" else ""

    s"$wikiPart $rdfPart"


  }

  def exportString(f: String => String): String = {
    val S = "$$"
    val d = wikiDelete.map(d =>
      s"${f(UpdatePattern.PAGE)}${S}${infoboxType}${S}${d.predicate}$S ${S}DELETE${S}")
    val i = wikiInsert.map(d =>
      s"${f(UpdatePattern.PAGE)}${S}${infoboxType}${S}${d.predicate}$S ${S}UPDATE${S}")

    d.mkString("\n") + "\n" + i.mkString("\n")
  }

}

object UpdatePattern {
  val LANGUAGE = "en"
  val STRING = "xsd:string"
  val PAGE = "?page"

  var lastVarName = "?@" // init with a char preceding A

  var WIKISIDE_UPDAE = "wiki"
  var RDFSIDE_UPDATE = "rdf"

  val WIKI_INSERT = "WikiInsert"
  val WIKI_DELETE = "WikiDelete"

  val RDF_INSERT = "RDFInsert"
  val RDF_DELETE = "RDFDelete"

  def wd(property:String ) = new Quad(LANGUAGE, null, PAGE, property, freshVar, WIKI_DELETE, STRING)
  def wd(property:String, value:String ) = new Quad(LANGUAGE, null, PAGE, property, value, WIKI_DELETE, STRING)
  def wdp(property:String ) = new UpdatePattern(PAGE, wikiDelete=HashSet(wd(property)))
  def wdp(property:String, value:String ) = new UpdatePattern(PAGE, wikiDelete=HashSet(wd(property,value)))


  def rd(property:String) = new Quad(LANGUAGE, null, PAGE, property, freshVar, RDF_DELETE, STRING)
  def rd(property:String, objct: String) = new Quad(LANGUAGE, null, PAGE, property, objct, RDF_DELETE, STRING)
  def rdp(property:String) = new UpdatePattern(PAGE, rdfDelete=HashSet(rd(property)))
  def rdp(property:String, objct: String) = new UpdatePattern(PAGE, rdfDelete = HashSet(rd(property,objct)))

  def wi(property:String ) = new Quad(LANGUAGE, null, PAGE, property, freshVar, WIKI_INSERT, STRING)
  def wi(property:String, value:String ) = new Quad(LANGUAGE, null, PAGE, property, value, WIKI_INSERT, STRING)
  def wip(property:String ) = new UpdatePattern(PAGE, wikiInsert=HashSet(wi(property)))
  def wip(property:String, value:String ) = new UpdatePattern(PAGE, wikiInsert=HashSet(wi(property,value)))

  def ri(property:String) = new Quad(LANGUAGE, null, PAGE, property, freshVar, RDF_INSERT, STRING)
  def ri(property:String, objct: String) = new Quad(LANGUAGE, null, PAGE, property, objct, RDF_INSERT, STRING)
  def rip(property:String) = new UpdatePattern(PAGE, rdfInsert=HashSet(ri(property)))
  def rip(property:String, objct: String) = new UpdatePattern(PAGE, rdfInsert = HashSet(ri(property,objct)))


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

  def fromWikiDML( dml: WikiDML ) : UpdatePattern = fromWikiDMLs(Seq(dml))

  def fromWikiDMLs( dmls: Seq[WikiDML] ) : UpdatePattern = {
    val up = new UpdatePattern()

    dmls.foreach(dml => {
      up.infoboxType = dml.infobox
      val (d,i) = wikiDMLToQuadPair(dml)
      d match {
        case Some(q) => up.wikiDelete.add(q)
        case None =>
      }
      i match {
        case Some(q) => up.wikiInsert.add(q)
        case None =>
      }
    })

    up
  }

  def wikiDMLToQuadPair( dml: WikiDML ) : (Option[Quad],Option[Quad]) = {
    dml.operation match {
      case "DELETE" => (Some(wd(dml.property,dml.oldValue)),None)
      case "INSERT" => (None, Some(wi(dml.property,dml.newValue)))
      case "UPDATE" => (Some(wd(dml.property,dml.oldValue)), Some(wi(dml.property,dml.newValue)))
      case _ => (None, None)
    }
  }

  def merge (ps : Seq[UpdatePattern]) : UpdatePattern = {
    if(ps.isEmpty) null
    else
      new UpdatePattern(infoboxType = ps.head.infoboxType
        , wikiDelete = Set() ++ ps.map( {x => x.wikiDelete} ).flatten
        , wikiInsert = Set() ++ ps.map( {x => x.wikiInsert} ).flatten
        , rdfDelete = Set() ++ ps.map( {x => x.rdfDelete} ).flatten
        , rdfInsert = Set() ++ ps.map( {x => x.rdfInsert} ).flatten
      )
  }

  def product[A](xs: Traversable[Traversable[A]]): Seq[Seq[A]] =
      xs.foldLeft(Seq(Seq.empty[A])){
        (x, y) => for (a <- x.view; b <- y) yield a :+ b }
}
