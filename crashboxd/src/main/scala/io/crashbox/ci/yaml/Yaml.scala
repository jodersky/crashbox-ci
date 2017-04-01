package io.crashbox.ci
package yaml

import java.util.{List => JList, Map => JMap}

import scala.collection.JavaConverters._

import org.yaml.snakeyaml.{DumperOptions, Yaml => SYaml}
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver

object Yaml {

  private def toYaml(yml: Any): YamlValue = yml match {
    case m: JMap[_, _] =>
      YamlMap(m.asScala.toMap.map { case (k, v) => k.toString -> toYaml(v) })
    case l: JList[_] => YamlSeq(l.asScala.toList.map(toYaml(_)))
    case s: String => YamlString(s)
    case other => throw new YamlFormatException("Unknown YAML type: " + other)
  }

  /** Strict parsing */
  def parse(data: String): YamlValue = {
    val resolver = new Resolver {
      override def addImplicitResolvers: Unit = {}
    }
    val yml = new SYaml(new Constructor(),
                        new Representer(),
                        new DumperOptions(),
                        resolver)
    val node = yml.load(data)
    toYaml(node)
  }

}
