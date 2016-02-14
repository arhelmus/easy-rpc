package me.archdev.rpc

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Tcp, Source}
import akka.util.ByteString
import autowire.Client
import boopickle.Default._

import scala.concurrent.{Future, ExecutionContext}

class RpcClient(host: String, port: Int)
  (implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) extends Client[ByteBuffer, Pickler, Pickler] {

  override def doCall(req: Request): Future[ByteBuffer] = {
    val source = Source.single(ByteString(RpcRequest.serialize(RpcRequest(req.path, req.args))))
    val connection = source.via(Tcp().outgoingConnection(host, port))

    connection.runFold(ByteString.empty) { case (data, accum) => accum ++ data }.map(_.asByteBuffer)
  }

  def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)

}
