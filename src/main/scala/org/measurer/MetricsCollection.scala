package org.measurer

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

sealed trait Metric
final case class Counter() extends Metric
trait CustomMetric extends Metric {
  def toBasic: Seq[Metric]
}

class MetricsRegistry {

  private val metricsMap: TrieMap[MetricIdentifier, Metric] = TrieMap.empty

  def registeredMetrics: scala.collection.Map[MetricIdentifier, Metric] = metricsMap.readOnlySnapshot()

  protected def register[M <: Metric](name: String, labels: Map[String, String], metric: M): M = {
    val id = MetricIdentifier(name, labels)
    metricsMap.putIfAbsent(id, metric) match {
      case Some(oldMetric) => throw MetricAlreadyDefinedException(id, oldMetric)
      case None => metric
    }
  }

  protected def register[M <: Metric](labels: Map[String, String], metric: M)(implicit name: sourcecode.Name): M = {
    register(name.value, labels, metric)
  }

  protected def register[M <: Metric](name: String, metric: M): M = {
    register(name, Map.empty, metric)
  }

  protected def register[M <: Metric](metric: M)(implicit name: sourcecode.Name): M = {
    register(name.value, metric)
  }

  protected def registerOrGet[M <: Metric](
    name: String,
    labels: Map[String, String],
    metric: M
  )(implicit classTag: ClassTag[M]): M = {
    val id = MetricIdentifier(name, labels)
    metricsMap.putIfAbsent(id, metric) match {
      case Some(oldMetric) =>
        if (classTag.runtimeClass.isInstance(oldMetric)) {
          oldMetric.asInstanceOf[M]
        } else {
          throw InvalidMetricType(id, oldMetric, metric)
        }

      case None =>
        metric
    }
  }


  protected def counter(name: String, labels: Map[String, String]): Counter = {
    register(name, labels, Counter())
  }

  protected def counter(labels: Map[String, String])(implicit name: sourcecode.Name): Counter = {
    counter(name.value, labels)
  }

  protected def counter(name: String): Counter = {
    counter(name, Map.empty)
  }

  protected def counter()(implicit name: sourcecode.Name): Counter = {
    counter(name.value, Map.empty)
  }


}

final case class MetricIdentifier(name: String, labels: Map[String, String])

case class MetricAlreadyDefinedException[M <: Metric](identifier: MetricIdentifier, metric: M)
  extends RuntimeException // TODO: message

case class InvalidMetricType[M <: Metric](identifier: MetricIdentifier, actualMetric: Metric, expectedMetric: M)
  extends RuntimeException // TODO: message

sealed trait MsgType
object MsgType {
  case object Foo extends MsgType
  case object Bar extends MsgType
}

class ModuleMetrics extends MetricsRegistry {

  val validMessages: Counter = counter()

  val published: Map[MsgType, Counter] = {
    val metricName = "published"
    Map(
      MsgType.Foo -> counter(metricName, Map("msgType" -> "foo")),
      MsgType.Bar -> counter(metricName, Map("msgType" -> "bar")),
    )
  }

  def error(errorType: String): Counter = {
    registerOrGet("error", Map("errorType" -> errorType), Counter())
  }
}

object TestApp extends App {
  val moduleMetrics = new ModuleMetrics

  moduleMetrics.published(MsgType.Foo)

  moduleMetrics.error("ololo")

  println(moduleMetrics.registeredMetrics)
}
