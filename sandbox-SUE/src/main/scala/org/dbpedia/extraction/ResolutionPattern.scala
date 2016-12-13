package org.dbpedia.extraction

import org.dbpedia.extraction.destinations.Quad

/**
  * Created by Vadim on 09.11.2016.
  */
class ResolutionPattern (
    val wikiPage: String, // should be URI, what is a relevant datatype
    var wikiDelete: Seq[Quad], // each triple represents a deleted Wiki property
    var wikiInsert: Seq[Quad], // each triple represents an inserted Wiki property using variables
    var rdfDelete: Seq[Quad],  // deleted dbpedia triples
    var rdfInsert: Seq[Quad], // inserted dbpedia triples using variable

    val infobox: String, // The affected infobox

    // relevant Input Quad + precondition + Axiom? => induced ResPattern

    //e.g. Axiom would be "Infobox Key Constraint"
    // .. or, "Mapping condition" -> induced ResPatter will include a WikiInsert / wikiDelete
    // ... or a particular TBox rule (e.g., disjointness /functionality)

    val infobox:String,

    val property:String,
    val oldValue: String=null,
    val newValue: String=null,
    val operation: String) {



  override def toString(): String = {
    if (operation == "UPDATE")
      return "ON wikiPage = " + wikiPage + "\n" +
        operation.toUpperCase() + " InfoboxTemplate(" + infobox + ")." + property + " = " + newValue + "\n" +
        "WHERE" + " InfoboxTemplate(" + infobox + ")." + property + " = " + oldValue + ";\n"
    else if (operation == "INSERT")
      return "ON wikiPage = " + wikiPage + "\n" +
        operation.toUpperCase() + " InfoboxTemplate(" + infobox + ")." + property + " = " + newValue + ";\n"
    else
      return "ON wikiPage = " + wikiPage + "\n" +
        operation.toUpperCase() + " InfoboxTemplate(" + infobox + ")." + property + ";\n"

  }

  def exportString(): String = {
    return wikiPage + "$$" + infobox + "$$" + property + "$$" + oldValue + "$$" + newValue + "$$" + operation + "$$"
  }

}
