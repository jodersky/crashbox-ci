package io.crashbox.ci

import java.net.URL


object Main extends Core
    with Schedulers
    with Builders
    with Parsers
    with Source
    with StreamStore {

  def main(args: Array[String]): Unit = {
    reapDeadBuilds()

    start(
      "random_build",
      new URL("file:///home/jodersky/tmp/dummy"),
      () => saveStream("random_build"),
      state => println(state)
    )
    Thread.sleep(15000)
    System.exit(0)
   }

}
