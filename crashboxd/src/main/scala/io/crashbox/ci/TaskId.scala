package io.crashbox.ci

case class TaskId(buildId: String, taskIdx: Int) {
  override def toString = s"$buildId#$taskIdx"
}
