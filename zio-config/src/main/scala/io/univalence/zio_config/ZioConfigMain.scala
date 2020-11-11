package io.univalence.zio_config

import zio.{ExitCode, URIO, ZIO, system}
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import ConfigDescriptor._
import ConfigSource._
import zio.config.derivation.describe
import zio.console.putStrLn

object ZioConfigMain extends zio.App {
  val arguments =
    List("--name=my-service", "--kafka.topics.output=output-topic")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      envs <- zio.system.envs
      _ <- ZIO {
        envs.foreach(println)
        println(arguments)
      }
      sourceEnv <- ConfigSource.fromSystemEnv(
        keyDelimiter = Some('_'),
        valueDelimiter = Some(',')
      )
      sourceArgs = ConfigSource.fromCommandLineArgs(arguments, keyDelimiter = Some('.'))
      taggedConfig: ConfigDescriptor[ServiceConfig] = serviceConfig.from(sourceArgs <> sourceEnv)
      _ <- putStrLn(
        s"${generateDocs(taggedConfig).toTable.asGithubFlavouredMarkdown}"
      )
      config <- ZIO.fromEither(zio.config.read(taggedConfig))
      _ <- putStrLn(s"$config")
    } yield {
      ()
    }).exitCode

  def serviceConfig: ConfigDescriptor[ServiceConfig] =
    descriptor[ServiceConfig]
}

case class KafkaConfig(
    bootstrapServers: String,
    topics: Map[String, String]
)

case class ServiceConfig(
    @describe("the name of the service")
    name: String,
    kafka: KafkaConfig
)
