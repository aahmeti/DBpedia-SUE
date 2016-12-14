package org.dbpedia.updateresolution

import com.hp.hpl.jena.sparql.core
import scala.collection.mutable

case class Update ( pattern: UpdatePattern,
                    instantiation: Map[String, String] )
{
  override def toString() = pattern.toString(replace)

  def toString( component:String ) : String = {pattern.toString(replace, component)}

  def exportString():String = pattern.exportString(replace)

  def replace(v:String):String = instantiation(v)

  def isEmpty : Boolean = false
}

object Update {

  def fromSPARQLAtoms( deletions: Seq[core.Quad]
                      ,insertions: Seq[core.Quad] ) : Update = {

    val all = (deletions ++ insertions)

    if( all.isEmpty ) {
      Update(UpdatePattern(), Map.empty)
    }
    else {
      val subj = all.head.asTriple.getSubject.toString
      val const = mutable.HashSet[String]()

      all.foreach(x => const.add(x.getObject.toString))
      val inv = const
        .zipWithIndex // (a,0),(b,1),(c,2),...
        .map { case (c, _) => (c, UpdatePattern.freshVar) }
        .toMap ++ Map((subj,"?page"))


      val up = UpdatePattern(subj)
      deletions.foreach(q => up.rdfDelete+= UpdatePattern.rd(q.getPredicate.toString,inv(q.getObject.toString)))
      insertions.foreach(q => up.rdfInsert+= UpdatePattern.ri(q.getPredicate.toString,inv(q.getObject.toString)))

      Update(up, inv.map(_.swap)) //invert the const-to-value map
    }
  }
}