package org.measurer

import java.lang.management.ManagementFactory

import scala.io.StdIn
import scala.jdk.CollectionConverters._
import scala.util.Try

object TestsApp extends App {

  val server = ManagementFactory.getPlatformMBeanServer

  val mbeans = server.queryMBeans(null, null).asScala

  val mbeansMap = mbeans.map { mbean =>
    val objectName = mbean.getObjectName
    val beanInfo = server.getMBeanInfo(mbean.getObjectName)
    val attrs = beanInfo.getAttributes.map { attr =>
      val attrName = attr.getName
      attrName -> Try(server.getAttribute(objectName, attr.getName)).toOption
    }.toMap
    objectName -> attrs
  }.toMap

  mbeansMap.foreach { case (k, v) =>
    println(k)
    println(v)
  }

  println("Press any key to exit...")
//  StdIn.readLine()
  println("Exit")
}
