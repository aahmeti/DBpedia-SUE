package org.dbpedia.updateresolution

import org.dbpedia.extraction.destinations.Quad


trait InducedUpdateReason{ def instance: Quad }

case class DisjointnessConstraintRepair( constraintc:Quad, instancec:Quad )
extends InducedUpdateReason
{
  val constraint:Quad = constraintc
  val instance:Quad = instancec
}

case class InfoboxKeyConstraintRepair( instancec:Quad )
  extends InducedUpdateReason
{
  val instance:Quad = instancec
}

