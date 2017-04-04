package bugapp

import java.util.concurrent.atomic.AtomicBoolean

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.sift.Discriminator
import ch.qos.logback.core.spi.ContextAwareBase

/**
  * Created by arhathq on 28.03.2017.
  */
class ActorBasedDiscriminator extends ContextAwareBase with Discriminator[LoggingEvent] {
  private val KEY = "reportActor"

  private val started = new AtomicBoolean(false)

  override def getDiscriminatingValue(event: LoggingEvent): String = {
    val loggerName = event.getLoggerName
    if(loggerName.endsWith("Actor"))
      loggerName
    else event.getLevel.toString
  }

  override def getKey: String = KEY

  override def start(): Unit = started.getAndSet(true)

  override def isStarted: Boolean = started.get

  override def stop(): Unit = started.getAndSet(false)
}