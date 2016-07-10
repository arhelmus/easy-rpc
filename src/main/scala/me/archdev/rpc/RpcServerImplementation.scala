package me.archdev.rpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString

import scala.concurrent.ExecutionContext

private[rpc] class RpcServerImplementation(router: Router.Router, parallelism: Int = 100)
  (implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) {

  def launch(host: String, port: Int) = {
    val flow = Flow[ByteString]
      .map(_.asByteBuffer)
      .map(RpcRequest.deserialize)
      .map(r => autowire.Core.Request(r.path, r.params))
      .mapAsync(parallelism)(router)
      .map(ByteString.apply)

    Tcp().bind(host, port).runForeach(_.handleWith(flow))
  }

}