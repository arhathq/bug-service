package bugapp

import java.util.Date

/**
  * @author Alexander Kuleshov
  */
case class Bug(id: String,
               severity: String,
               priority: String,
               status: String,
               resolution: String,
               reporter: String,
               opened: String,
               assignee: String,
               changed: String,
               product: String,
               component: String,
               environment: String,
               summary: String,
               hardware: String
              )

case class BugsError(message: String) extends Exception(message)