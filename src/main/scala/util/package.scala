import scalaz.zio.IO
package object util {

  def putStrLn(str: String): IO[Nothing, Unit] = IO sync { println(str) }

}
