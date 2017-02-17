package bugapp.repository

import java.nio.file.Paths

import bugapp.EmployeeConfig

import scala.collection.mutable
import scala.io.Source

/**
  *
  */
trait EmployeeRepository {

  def getEmployee(email: String): Option[Employee]

}

class FileEmployeeRepository extends EmployeeRepository with EmployeeConfig {

  private val employees = mutable.Map.empty[String, Employee]

  Source.fromFile(Paths.get(repositoryPath).toFile).getLines.foreach { line =>
    val values = line.split(",").map(_.trim)
    val email = values(0)
    val department = values(1)
    employees += (email -> Employee(email, department))
  }

  override def getEmployee(email: String): Option[Employee] = employees.get(email)
}

case class Employee(email: String, department: String)
