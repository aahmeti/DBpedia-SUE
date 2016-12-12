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
        case UpdateComponent.WikiInsert => pattern.wikiInsert
        case UpdateComponent.WikiDelete => pattern.wikiDelete
        case UpdateComponent.RdfInsert => pattern.rdfInsert
        case UpdateComponent.RdfDelete => pattern.rdfDelete
      }
  }

}

