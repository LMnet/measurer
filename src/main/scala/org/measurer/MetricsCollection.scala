package org.measurer

sealed trait Metric
case class SingleMetric[M](metric: M) extends Metric
case class LabeledMetric[M](metrics: Map[Label, M]) extends Metric

case class MetricsCollection(
  metrics: Map[String, Metric],
)

case class Label(name: String, value: String)
