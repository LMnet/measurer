package org.measurer

import com.codahale.metrics.Counter

sealed trait NamedMetric {
  def report: String = toString // TODO: tmp
}
object NamedMetric {
  case class Single[M](name: String, metric: M) extends NamedMetric
  case class Labeled[M](name: String, labels: Map[String, String], metric: M) extends NamedMetric
}

class MetricsCollection(
  prefix: Option[String],
  metrics: Seq[NamedMetric],
)
object MetricsCollection {
  def apply(metrics: Seq[NamedMetric]) = new MetricsCollection(None, metrics)
}

case class LabeledMetric()

class MetricsRegistry {
  protected def named[M](metric: M)(implicit name: sourcecode.Name): M = ???
  protected def named[M](name: String, metric: M): M = ???

  protected def labeled[M](labels: Map[String, String], metric: M): Map[Map[String, String], M]
}



class ModuleMetrics extends MetricsRegistry {
  val validMessages = named(new Counter())
}
