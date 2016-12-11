package org.dbpedia.extraction

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.UpdateComponent._

class Update (
   pattern: ResolutionPattern,
   instantiation: Map[String, Value]
)
{
  def getMaterializedQuads( component : UpdateComponent ): Seq[Quad] = {
      component match {
        case WikiInsert => pattern.wikiInsert
        case WikiDelete => pattern.wikiDelete
        case RdfInsert => pattern.rdfInsert
        case RdfDelete => pattern.rdfDelete
      }
  }

}

