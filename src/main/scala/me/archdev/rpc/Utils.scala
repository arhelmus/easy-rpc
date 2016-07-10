package me.archdev.rpc

import akka.util.ByteString

object Utils {

  def serialize[T](data: T)(implicit state: PickleState, p: Pickler[T]) = ByteString(Pickle.intoBytes(data))

}
