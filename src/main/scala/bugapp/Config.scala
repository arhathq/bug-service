package bugapp

import com.typesafe.config.ConfigFactory

trait Config {

  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")
  private val bugzillaConfig = config.getConfig("bugzilla")

  val akkaConfig = config.getConfig("akka")

  val httpHost = httpConfig.getString("host")
  val httpPort = httpConfig.getInt("port")

  val bugzillaUrl = bugzillaConfig.getString("url")
  val bugzillaUsername = bugzillaConfig.getString("username")
  val bugzillaPassword = bugzillaConfig.getString("password")

}
