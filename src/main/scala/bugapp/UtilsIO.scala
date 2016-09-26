package bugapp

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
  *
  */
object UtilsIO {

  private val repositoryDateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd")

  def write(output: String, data: Array[Byte]): Unit = {

    Some(new RandomAccessFile(output, "rw").getChannel).foreach { fc =>
      try {
        fc.write(ByteBuffer.wrap(data))
      } catch {
        case t: Throwable => println(s"Error write to $output")
      } finally {
        fc.close()
      }
    }
  }

  def write(output: String, data: String): Unit = write(output, data.getBytes())

  def ifFileExists(path: String) = Files.exists(Paths.get(path))

  def createDirectoryIfNotExists(path: String) =
    if (!ifFileExists(path)) Files.createDirectory(Paths.get(path))

  def bugzillaDataPath(rootPath: String, date: LocalDate): String = s"$rootPath/${repositoryDateFormat.format(date)}"
}
