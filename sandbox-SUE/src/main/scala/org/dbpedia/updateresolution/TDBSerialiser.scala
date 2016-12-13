package org.dbpedia.updateresolution

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import at.tuwien.dbai.rewriter.TripleStore
import com.hp.hpl.jena.query.ReadWrite
import com.hp.hpl.jena.tdb.TDBFactory
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.destinations.formatters.TerseFormatter

/**
 * Created by aahmeti on 13/12/2016.
 */
class TDBSerialiser {

  val download_directory = "./data/downloads"
  val dataset = TDBFactory.createDataset(download_directory + "/" + "TDB")

  def store(quads: Seq[Quad]): Unit = {

    dataset.begin(ReadWrite.WRITE)
    val model = dataset.getDefaultModel()
    val ts = new TripleStore(model)
    ts.setDataset(dataset)

    val t = new TerseFormatter(false, true)

    val sb = new StringBuilder()

    for (quad <- quads) {
      sb.append( t.render(quad) )
    }

    model.read( new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), null, "N3" )


  }

}
