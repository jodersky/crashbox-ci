package io.crashbox.ci

import java.io.{ ByteArrayOutputStream, File }
import java.nio.file.Files

object IOUtil {

  def withTempDir[A](action: File => A): A = {
    def rm(parent: File): Unit = if (parent.isDirectory) {
      parent.listFiles.foreach{ child =>
        rm(child)
      }
    }
    val dir = Files.createTempDirectory("crashbox-test").toFile
    try action(dir)
    finally rm(dir)
  }

  def withTempStream[A](action: ByteArrayOutputStream => A, size: Int = 1024): A = {
    val out = new ByteArrayOutputStream(size)
    try action(out)
    finally out.close()
  }

  def withTemp[A](action: (File, ByteArrayOutputStream) => A, size: Int = 1024): A = {
    withTempDir { d =>
      withTempStream { s =>
        action(d, s)
      }
    }
  }

}
