package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.WikiDML
import org.dbpedia.extraction.destinations.{Quad,Dataset}
import org.dbpedia.extraction.wikiparser.TemplateNode

/**
 * TODO: generic type may not be optimal.
 */
trait Extractor[-N] {
  
    /**
     * @param input The source node
     * @param subjectUri The subject URI of the generated triples
     * @param context The page context which holds the state of the extraction.
     * @return A graph holding the extracted data
     */
    def extract(input: N, subjectUri: String, context: PageContext): Seq[Quad]

    def extractWithInfoboxChanges(input : N, subjectUri : String, context : PageContext, wikiUpdate: Seq[WikiDML]): Seq[Quad] = { Seq.empty }

  /**
   *
   * @param input The source node
   * @param subjectUri The subject URI of the generated triples (OPTIONAL)
   * @param context The page context which data holds the state of the extraction (OPTIONAL)
   * @param updateSubjectUri The subject URI of the update (OPTIONAL)
   * @param updatePropertyUri The property URI of the update
   * @param updateValue The object value (URI?) of the update
   * @param updateOperation The update operation
   * @return
   */
    def resolve(input: N, subjectUri: String=null, context:PageContext=null, updateSubjectUri: String=null, updatePropertyUri : String, updateValue : String, updateOperation:String): Seq[WikiDML]  = { Seq.empty }
    /**
     * Datasets generated by this extractor. Used for serialization. If a mapping implementation
     * does not return all datasets it produces, serialization may fail.
     */
    val datasets: Set[Dataset]
}