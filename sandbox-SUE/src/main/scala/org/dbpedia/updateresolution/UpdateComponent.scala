package org.dbpedia.updateresolution

/**
  * Created by Vadim on 09.11.2016.
  */
object UpdateComponent extends Enumeration {
  type UpdateComponent = Value
  val WikiInsert, WikiDelete, RdfInsert, RdfDelete = Value
}
