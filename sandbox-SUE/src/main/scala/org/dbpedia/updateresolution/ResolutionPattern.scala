package org.dbpedia.updateresolution

import org.dbpedia.extraction.destinations.Quad

/**
  * Created by Vadim on 09.11.2016.
  */
class ResolutionPattern (
    val infoboxType: String, // should be URI, what is a relevant datatype
    val wikiDelete: Seq[Quad], // each triple represents a deleted Wiki property
    val wikiInsert: Seq[Quad], // each triple represents an inserted Wiki property using variables
    val rdfDelete: Seq[Quad],  // deleted dbpedia triples
    val rdfInsert: Seq[Quad], // inserted dbpedia triples using variable
    val inducedBy: InducedUpdateReason )
  {

  override def toString(): String = toString(x=>x)

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
