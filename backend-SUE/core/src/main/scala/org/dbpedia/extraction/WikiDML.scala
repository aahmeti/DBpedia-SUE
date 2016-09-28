package org.dbpedia.extraction

class WikiDML(val wikiPage:String, val infobox: String, val property:String, val oldValue: String=null, val newValue: String=null, val operation: String) {

  override def toString(): String =
  {
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
  def exportString():String =
  {
    return wikiPage + "$$" + infobox + "$$" + property + "$$" + oldValue + "$$" + newValue + "$$" + operation + "$$"
  }
}
