package bugapp

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths, StandardCopyOption}

import scala.util.Try

/**
  *
  */
object UtilsIO {

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

  def bugzillaDataPath(rootPath: String): String = s"$rootPath/repo"

  def move(from: String, to: String): Boolean = {
    Try(Files.move(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING)).isSuccess
  }
}