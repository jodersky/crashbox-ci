package io.crashbox.ci

import java.io.File
import java.nio.file.Files

object TestUtil {

  def withTempDir[A](f: File => A): A = {
    val dir = Files.createTempDirectory("crashbox-test").toFile
    try f(dir)
    finally dir.delete()
  }

}
