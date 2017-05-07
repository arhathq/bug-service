package bugapp

import java.time.OffsetDateTime
import java.util
import java.util.Properties

import bugapp.report.ReportTypes.ReportType

import collection.JavaConverters._
import com.typesafe.config.{ConfigException, ConfigFactory, Config => TypesafeConfig}

import scala.util.control.Exception.catching

/**
  * Application configuration
  */
trait Config {

  private[bugapp] val config = ConfigFactory.load()

  private[bugapp] val catchMissing = catching(classOf[ConfigException.Missing])

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

  def optionalValue[T](path: String)(implicit config: TypesafeConfig): Option[T] =
    if (config.hasPathOrNull(path)) Some(config.getValue(path).asInstanceOf[T]) else None
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

  val searchStartDate = OffsetDateTime.parse(bugzillaConfig.getString("start-date"))
  val bugLimit = bugzillaConfig.getInt("bug-limit")
  val rootPath = bugzillaConfig.getString("repository-path")
  val repositoryFile = bugzillaConfig.getString("repository-file")
  val fetchTimeout = bugzillaConfig.getInt("fetch-timeout")

  val environment = bugzillaConfig.getString("environment")
  val excludedProducts = bugzillaConfig.getStringList("excludedProducts").asScala
  val excludedComponents = bugzillaConfig.getStringList("excludedComponents").asScala
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

  val reportTemplate: (ReportType) => String = (reportType) => reportTypes(reportType.name)("template").asInstanceOf[String]

}

trait EmployeeConfig extends Config {

  private val employeeConfig = config.getConfig("employees")

  val repositoryPath = employeeConfig.getString("path")
}

trait MailerConfig extends Config {

  private implicit val mailerConfig = config.getConfig("mailer")

  val mailUsername: Option[String] = catchMissing opt {
    Option(mailerConfig.getString("mail.username"))
  } getOrElse None
  val mailPassword: Option[String] = catchMissing opt {
    Option(mailerConfig.getString("mail.password"))
  } getOrElse None

  val mailProps = new Properties()

}

trait EmailConfig extends Config {

  private val emailsConfig = config.getConfig("emails")

  val emails = getConfigMap(emailsConfig)

  val from = emailsConfig.getString("from")
  val to: (String) => Array[String] = (emailId) => emails(emailId)("to").asInstanceOf[util.ArrayList[String]].asScala.toArray
  val cc: (String) => Array[String] = (emailId) => emails(emailId)("cc").asInstanceOf[util.ArrayList[String]].asScala.toArray

}