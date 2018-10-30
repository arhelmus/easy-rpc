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
Think twice before use, its not supported!
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

## Messaging protocol
Messaging between client and server builded on top of binary serialization with [boopickle](https://github.com/ochrons/boopickle) library. To do request you must send data model that will look like:   

* __id: Long__ - unique identifier of request, used in asyncrony client implementation
* __path: Seq[String]__ - elements of method path. As example for method 'test' in class 'me.archdev.TestMethod' it will be like Seq('me', 'archdev', 'TestMethod', 'test')
* __params: Map[String, ByteBuffer]__ - serialized parameters map where key is method parameter name

As response you may took model that contains:
* __id: Long__ - unique identifier of request for client purposes
* __data: Option[ByteBuffer]__ - serialized method response or None, if something goes wrong
* __error: Option[ErrorProtocol]__ - error description, if it has. You can see error types [here](https://github.com/ArchDev/easy-rpc/blob/master/src/main/scala/me/archdev/rpc/protocol/ErrorProtocol.scala)


## Copyright
Copyright (C) 2015 Arthur Kushka.  
Distributed under the MIT License.

