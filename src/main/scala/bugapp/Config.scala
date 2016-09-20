package bugapp

import com.typesafe.config.ConfigFactory

trait Config {

  private[bugapp] val config = ConfigFactory.load()

}

trait HttpConfig extends Config {

  private val httpConfig = config.getConfig("http")

  val httpHost = httpConfig.getString("host")
  val httpPort = httpConfig.getInt("port")
}

trait BugzillaConfig extends Config {

  private val bugzillaConfig = config.getConfig("bugzilla")

  val bugzillaUrl = bugzillaConfig.getString("url")
  val bugzillaUsername = bugzillaConfig.getString("username")
  val bugzillaPassword = bugzillaConfig.getString("password")

  val fetchPeriod = bugzillaConfig.getInt("fetch-period-in-weeks")
  val rootPath = bugzillaConfig.getString("repository-path")
  val repositoryFile = bugzillaConfig.getString("repository-file")
  val fetchTimeout = bugzillaConfig.getInt("fetch-timeout")

  val excludedProducts = bugzillaConfig.getStringList("excludedProducts")
  val excludedComponents = bugzillaConfig.getStringList("excludedComponents")
}

trait AkkaConfig extends Config {

  val akkaConfig = config

}

trait ReportConfig extends Config {

  val maxJobs = 10


}