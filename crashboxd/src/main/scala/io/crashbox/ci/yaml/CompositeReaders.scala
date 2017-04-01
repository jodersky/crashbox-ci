package io.crashbox.ci
package yaml

trait CompositeReaders {

  implicit def mapReader[V: YamlReader] = new YamlReader[Map[String, V]] {
    override def read(yml: YamlValue) = yml match {
      case YamlMap(m) =>
        m.map {
          case (key, value) =>
            key -> value.convertTo[V]
        }
      case YamlString.Empty => Map.empty[String, V]
      case _ => formatError(yml, "mapping")
    }
  }

  implicit def seqReader[A: YamlReader] = new YamlReader[Seq[A]] {
    override def read(yml: YamlValue) = yml match {
      case YamlSeq(elements) =>
        elements.map { v =>
          v.convertTo[A]
        }
      case YamlString.Empty => Seq.empty[A]
      case _ => formatError(yml, "sequence")
    }
  }

}
