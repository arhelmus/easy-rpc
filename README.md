Easy RPC
=========================

This library implements easy TCP RPC service with binary serialization.
Main goal of it, just provide ready solution for communication between services.

### Features:
* Binary serialization (based on boopickle)
* TCP as data channel (based on akka-streams)
* Backpressure
* Asynchrony

### Usage example:
```
trait EchoService {
  def echo(message: String): String
}

class EchoServiceImpl extends EchoService {
  override def echo(message: String): String = message
}

object Application extends App {

  import autowire._
  import boopickle.Default._
  
  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val rpcHost = "localhost"
  val rpcPort = 20000

  val rpcServer = new RpcServer(Router.route[EchoService](new EchoServiceImpl))
  rpcServer.start(rpcHost, rpcPort)
  
  val rpcClient = new RpcClient(rpcHost, rpcPort)
  
  val message = "echo"
  rpcClient[EchoService].echo(message).call().map { response =>
    println(response)
  }
  
}
```

### Copyright
Copyright (C) 2015 Arthur Kushka.
Distributed under the MIT License.
