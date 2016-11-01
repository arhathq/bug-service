package bugapp

import com.typesafe.config.ConfigFactory
import com.typesafe.config.{Config => TypesafeConfig}

trait Config {

  private[bugapp] val config = ConfigFactory.load()

  /*
   * Parses following config into a Map
   *
   *  config = {
   *    key1 = {
   *      value1 = "v1"
   *      value2 = "v2"
   *    }
   *
   *    key2 = {
   *      value1 = "v3"
   *      value2 = "v4"
   *    }
   *  }
   */
  def getConfigMap(config: TypesafeConfig): Map[String, Map[String, Any]] = {
    import scala.collection.JavaConversions._
    config.entrySet().
      map { entry =>
        val keys = entry.getKey.split("\\.").toSeq
        keys.head -> (keys.tail.mkString(".") -> entry.getValue.unwrapped())
      } groupBy(_._1) map(kv => kv._1 -> kv._2.map(kv1 => kv1._2).toMap)
  }
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

  private val reportConfig = config.getConfig("reports")

  val maxJobs = reportConfig.getInt("maxJobs")
  val fopConf = reportConfig.getString("fopConf")
  val reportDir = reportConfig.getString("reportDir")
  val reportTypes = getConfigMap(reportConfig.getConfig("types"))

  val reportTemplate: (String) => String = (reportType) => reportTypes(reportType)("templateDir").asInstanceOf[String]

}