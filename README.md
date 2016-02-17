Easy RPC
=========================

[![Build Status](https://travis-ci.org/ArchDev/easy-rpc.svg?branch=master)](https://travis-ci.org/ArchDev/easy-rpc)

Implementation of TCP RPC communication with binary serialization based on [Autowire](https://github.com/lihaoyi/autowire).
Main goal of this library - just provide ready solution for communication between services.

### Features:
* Binary serialization (based on [boopickle](https://github.com/ochrons/boopickle))
* TCP as data channel (based on [akka-streams](https://github.com/akka/akka))
* Backpressure
* Asynchrony

### Dependency:
```scala
resolvers += Resolver.sonatypeRepo("public")
libraryDependencies += "me.archdev" %% "easy-rpc" % "1.0.1"
```

## Usage example:
```scala
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

  val rpcServer = new RpcServer(Router.route[EchoService](new EchoServiceImpl), backpressure = 42)
  rpcServer.start(rpcHost, rpcPort)
  
  val rpcClient = new RpcClient(rpcHost, rpcPort)
  
  val message = "echo"
  rpcClient[EchoService].echo(message).call().map { response =>
    println(response)
  }
  
}
```

## Copyright
Copyright (C) 2015 Arthur Kushka.  
Distributed under the MIT License.

