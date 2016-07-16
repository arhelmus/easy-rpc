package me.archdev.rpc.internal

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

import me.archdev.rpc.{InvalidProtocolException, RemoteRpcException}
import me.archdev.rpc.protocol.{MethodNotFoundError, _}

import scala.collection.mutable
import scala.concurrent.Promise

/**
  * Helper class that contains specific for async rpc implementation stuff.
  */
trait AsyncRpcClientHelper {

  val idCounter = new AtomicLong(0)
  val promiseMapping: mutable.Map[Long, Promise[ByteBuffer]] = mutable.Map()

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
      case RpcResponse(id, None, Some(error)) =>
        println("Bad things was happened")
      case RpcResponse(id, _, _) =>
        println("Bad things was happened")
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

}
