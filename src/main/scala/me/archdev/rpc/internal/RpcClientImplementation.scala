package me.archdev.rpc.internal

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, _}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import autowire.Client
import me.archdev.rpc._
import me.archdev.rpc.protocol._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

private[rpc] class RpcClientImplementation(tcpConnection: OutgoingTcpConnection)
  (implicit as: ActorSystem, ec: ExecutionContext, m: Materializer) extends Client[ByteBuffer, Pickler, Pickler] {

  val idCounter = new AtomicLong(0)
  val promiseMapping: mutable.Map[Long, Promise[ByteBuffer]] = mutable.Map()

  private val bufferSize = 100
  private val overflowStrategy = OverflowStrategy.dropNew

  val flow = Source.queue[RpcRequest](bufferSize, overflowStrategy)
    .map(RpcRequest.serialize)
    .map(ByteString.apply)
    .via(tcpConnection)
    .map(_.asByteBuffer)
    .map(RpcResponse.deserialize)
    .toMat(Sink.foreach[RpcResponse](finishRPCRequest))(Keep.left)
    .run()

  override def doCall(req: Request): Future[ByteBuffer] =
    formRPCRequest(req.path, req.args) match {
      case (request, promise) =>
        flow.offer(request).flatMap(queueOfferResultToResultFuture(_, promise))
    }

  def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)

  def formRPCRequest(path: Seq[String], args: Map[String, ByteBuffer]) = {
    val rpcRequest = RpcRequest(idCounter.getAndAdd(1), path, args)
    val promise = Promise[ByteBuffer]
    promiseMapping.put(rpcRequest.id, promise)

    rpcRequest -> promise
  }

  def finishRPCRequest(rpcResponse: RpcResponse): Unit =
    rpcResponse match {
      case RpcResponse(id, Some(data), None) if promiseMapping.contains(id) =>
        promiseMapping(id).success(data)
        promiseMapping.remove(id)
      case RpcResponse(id, None, Some(error)) if promiseMapping.contains(id) =>
        promiseMapping(id).failure(formExceptionFromError(error))
        promiseMapping.remove(id)
    }

  private def formExceptionFromError(error: ErrorProtocol) =
    error match {
      case e: ExceptionIsThrownError =>
        RemoteRpcException(e.name, e.exMessage, e.stackTrace)
      case e: InvalidMethodParametersError =>
        InvalidProtocolException(e.message)
      case e: MethodNotFoundError =>
        InvalidProtocolException(e.message)
    }

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