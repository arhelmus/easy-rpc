Easy RPC
=========================

[![Build Status](https://travis-ci.org/ArchDev/easy-rpc.svg?branch=master)](https://travis-ci.org/ArchDev/easy-rpc)

Implementation of TCP RPC communication with binary serialization.
Main goal of this library - just provide ready solution for communication between services.

### Features:
* Binary serialization (based on [boopickle](https://github.com/ochrons/boopickle))
* TCP as data channel (based on [akka-streams](https://github.com/akka/akka))
* Backpressure
* Asynchrony

### Dependency:
```scala
resolvers += Resolver.sonatypeRepo("public")
libraryDependencies += "me.archdev" %% "easy-rpc" % "1.1.0"
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
  import me.archdev.rpc._

  implicit val system = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val rpcHost = "localhost"
  val rpcPort = 20000

  val rpcServer = new RpcServer(Router.route[EchoService](new EchoServiceImpl))
  rpcServer.launch(rpcHost, rpcPort)

  val rpcClient = new RpcClient[EchoService](rpcHost, rpcPort)

  val message = "echo"
  rpcClient.echo(message).call().map { response =>
    println(response)
  }

}
```

## In development:
* Signing more that one remote service on single port;
* Auto-reconnect of client on any connection issue;
* Inclusion of autowire into library codebase to provide better user interface.

## Copyright
Copyright (C) 2015 Arthur Kushka.  
Distributed under the MIT License.

