package org.dbpedia.updateresolution

class Update (
               pattern: ResolutionPattern,
               instantiation: Map[String, String]
             )
{

  override def toString() = pattern.toString(replace)

  def exportString():String = pattern.exportString(replace)

  def replace(v:String):String = instantiation(v)

}

