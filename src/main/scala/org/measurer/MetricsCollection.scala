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
  private val registriesMap: TrieMap[MetricIdentifier, MetricsRegistry] = TrieMap.empty

  def registeredMetrics: scala.collection.Map[MetricIdentifier, Metric] = {
    val topLevelMetrics = metricsMap.readOnlySnapshot()
    val children = registriesMap.readOnlySnapshot()
    val childrenMetrics = children.flatMap { case (registryId, registry) => // TODO: id collisions
      val metrics = registry.registeredMetrics
      if (registryId.name != "" || registryId.labels.nonEmpty) {
        val prefix = if (registryId.name != "") s"${registryId.name}-" else ""
        metrics.map { case (id, metric) =>
          id.copy(
            name = prefix + id.name,
            labels = id.labels ++ registryId.labels, // TODO: labels collision
          ) -> metric
        }
      } else metrics
    }
    topLevelMetrics ++ childrenMetrics
  }

  def register[M <: Metric](name: String, labels: Map[String, String], metric: M): M = {
    val id = MetricIdentifier(name, labels)
    metricsMap.putIfAbsent(id, metric) match {
      case Some(oldMetric) => throw MetricAlreadyDefinedException(id, oldMetric)
      case None => metric
    }
  }

  def register[M <: Metric](labels: Map[String, String], metric: M)(implicit name: sourcecode.Name): M = {
    register(name.value, labels, metric)
  }

  def register[M <: Metric](name: String, metric: M): M = {
    register(name, Map.empty, metric)
  }

  def register[M <: Metric](metric: M)(implicit name: sourcecode.Name): M = {
    register(name.value, metric)
  }

  def registerOrGet[M <: Metric](
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


  def registerRegistry[M <: MetricsRegistry](name: String, labels: Map[String, String], registry: M): M = {
    val id = MetricIdentifier(name, labels)
    registriesMap.putIfAbsent(id, registry) match {
      case Some(oldRegistry) => throw RegistryAlreadyDefinedException(id, oldRegistry)
      case None => registry
    }
  }


  def counter(name: String, labels: Map[String, String]): Counter = {
    register(name, labels, Counter())
  }

  def counter(labels: Map[String, String])(implicit name: sourcecode.Name): Counter = {
    counter(name.value, labels)
  }

  def counter(name: String): Counter = {
    counter(name, Map.empty)
  }

  def counter()(implicit name: sourcecode.Name): Counter = {
    counter(name.value, Map.empty)
  }
}

final case class MetricIdentifier(name: String, labels: Map[String, String])

case class MetricAlreadyDefinedException[M <: Metric](identifier: MetricIdentifier, metric: M)
  extends RuntimeException // TODO: message

case class RegistryAlreadyDefinedException[M <: MetricsRegistry](identifier: MetricIdentifier, registry: M)
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
  val submoduleMetrics = new MetricsRegistry {
    val foo = counter("bar")
  }

  val moduleMetrics = new ModuleMetrics {
    val fooSubmodule = registerRegistry("foo", Map("submodule" -> "foo"), submoduleMetrics)
  }

  moduleMetrics.published(MsgType.Foo)

  moduleMetrics.error("ololo")

  println(moduleMetrics.registeredMetrics)
}
