package me.archdev.rpc.internal

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, _}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import autowire.Client
import me.archdev.rpc._
import me.archdev.rpc.protocol._

import scala.concurrent.{ExecutionContext, Future, Promise}

private[rpc] class RpcClientImplementation(tcpConnection: OutgoingTcpConnection)
  (bufferSize: Int = 100, overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) extends Client[ByteBuffer, Pickler, Pickler] with AsyncRpcClientHelper {

  override def doCall(req: Request): Future[ByteBuffer] =
    formRPCRequest(req.path, req.args) match {
      case (request, promise) =>
        rpcStream.offer(request).flatMap(queueOfferResultToResultFuture(_, promise))
    }

  override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)

  private val flow = Flow[RpcRequest]
    .map(serializeRpcRequest)
    .via(tcpConnection)
    .map(deserializeRpcResponse)

  private def startRpcStream() =
    Source.queue[RpcRequest](bufferSize, overflowStrategy).via(flow)
      .toMat(Sink.foreach[RpcResponse](finishRPCRequest))(Keep.left)
      .run()

  var rpcStream = startRpcStream()

  private def serializeRpcRequest(rpcRequest: RpcRequest): ByteString =
    ByteString(RpcRequest.serialize(rpcRequest))

  private def deserializeRpcResponse(byteString: ByteString): RpcResponse =
    RpcResponse.deserialize(byteString.asByteBuffer)

  private def queueOfferResultToResultFuture(queueOfferResult: QueueOfferResult, promise: Promise[ByteBuffer]) =
    queueOfferResult match {
      case Enqueued =>
        promise.future
      case Dropped =>
        promise.failure(new RuntimeException("RPC request is dropped from stream")).future
      case Failure(ex) =>
        promise.failure(ex).future
      case QueueClosed =>
        promise.failure(new RuntimeException("Rpc request stream is already closed")).future
    }

}