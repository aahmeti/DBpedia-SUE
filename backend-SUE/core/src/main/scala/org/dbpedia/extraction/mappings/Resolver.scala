package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.wikiparser.PageNode

import scala.collection.mutable.ArrayBuffer

trait Resolver[-N] extends Extractor[N] {

  def resolve(input: N, updatePropertyUri : String): Seq[WikiDML]

}
