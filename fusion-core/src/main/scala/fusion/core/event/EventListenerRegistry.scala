package fusion.core.event

import java.util.Objects

trait EventListenerRegistry[E <: AnyRef] {
  protected var _event: E  = _
  protected var _listeners = List[E => Unit]()

  def addListener(listener: E => Unit): Unit = synchronized {
    if (Objects.nonNull(_event)) {
      listener(_event)
    }
    _listeners ::= listener
  }

  def complete(event: E): Unit = synchronized {
    if (Objects.nonNull(_event)) {
      throw new IllegalStateException(s"$event 只能被调用一次")
    }
    _event = event
    dispatch()
  }

  protected def dispatch(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = _listeners.foreach(_.apply(_event))
    }).start()
  }
}