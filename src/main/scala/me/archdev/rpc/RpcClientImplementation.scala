package me.archdev.rpc

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import autowire.Client

import scala.concurrent.{ExecutionContext, Future}

private[rpc] class RpcClientImplementation(tcpConnection: OutgoingTcpConnection)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) extends Client[ByteBuffer, Pickler, Pickler] {

  // TODO: WTF, here returned only first one response. Must be rewritten.
  override def doCall(req: Request): Future[ByteBuffer] =
    Source.single(RpcRequest(req.path, req.args))
      .map(RpcRequest.serialize)
      .map(ByteString.apply)
      .via(tcpConnection)
      .map(_.asByteBuffer)
      .runWith(Sink.last)

  def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)

}