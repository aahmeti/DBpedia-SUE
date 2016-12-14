package org.dbpedia.updateresolution

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import at.tuwien.dbai.rewriter.TripleStore
import com.hp.hpl.jena.query.ReadWrite
import com.hp.hpl.jena.tdb.TDBFactory
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import org.openrdf.query.Dataset

import scala.collection.mutable.ArrayBuffer

/**
 * Created by aahmeti on 13/12/2016.
 */
class TDBSerialiser
(val download_directory:String="./data/downloads")
{
  val dataset = TDBFactory.createDataset(download_directory + "/" + "TDB")
  var ts:TripleStore = null

  def store(quads: Seq[Quad]): Unit = {

    dataset.begin(ReadWrite.WRITE)
    val model = dataset.getDefaultModel()
    ts = new TripleStore(model)
    ts.setDataset(dataset)

    val t = new TerseFormatter(false, true)

    val sb = new StringBuilder()

    for (quad <- quads) {
      sb.append( t.render(quad) )
    }

    model.read( new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), null, "N3" )
  }

  def query(query:String): Unit = {

    println(ts.runQuery(query))

  }

}


