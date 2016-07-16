package me.archdev.rpc.protocol

import autowire.Error.InvalidInput
import autowire.Error.Param.{Invalid, Missing}

/**
  * Base trait for error distribution.
  */
sealed trait ErrorProtocol {
  val message: String
}

/**
  * Indicates that exception was been thrown on server.
  */
case class ExceptionIsThrownError(name: String, exMessage: String, stackTrace: Seq[String]) extends ErrorProtocol {
  val message = "Rpc invocation exception. Exception is thrown."
}

/**
  * Indicates that something unpredictable is happens.
  */
case class ErrorIsOccurred(name: String, errorMessage: String, stackTrace: Seq[String]) extends ErrorProtocol {
  val message = "Unexpected error occurred."
}

/**
  * Indicates that requested class or method does not exists.
  */
case class MethodNotFoundError(classPath: String, method: String) extends ErrorProtocol {
  val message = s"Rpc invocation exception. Cannot find method '$method' in class '$classPath'."
}

/**
  * Indicates that request contains invalid parameters for method execution.
  */
case class InvalidMethodParametersError(invalidParameters: Seq[String]) extends ErrorProtocol {
  val message = s"Rpc invocation exception. Invalid method parameters: ${invalidParameters.mkString(", ")}."
}

object ExceptionIsThrownError {

  /**
    * Build error from exception.
    */
  def apply(throwable: Throwable) =
    new ExceptionIsThrownError(
      throwable.getClass.getSimpleName,
      throwable.getMessage,
      throwable.getStackTrace.map(_.toString)
    )

}

object ErrorIsOccurred {

  /**
    * Build error from exception.
    */
  def apply(throwable: Throwable) =
    new ErrorIsOccurred(
      throwable.getClass.getSimpleName,
      throwable.getMessage,
      throwable.getStackTrace.map(_.toString)
    )

}

object MethodNotFoundError {

  /**
    * Extracts class path and method that cannot be found from match error.
    */
  def apply(matchError: MatchError) = {
    val path = matchError.getMessage().drop(15).takeWhile(_ != ')').split(",").map(_.trim)
    new MethodNotFoundError(path.dropRight(1).mkString("."), path.last)
  }

}

object InvalidMethodParametersError {

  /**
    * Extracts field names from [[autowire.Error.InvalidInput]] list.
    */
  def apply(invalidInputEx: InvalidInput) =
    new InvalidMethodParametersError(invalidInputEx.exs.map {
      case Missing(p) => p
      case Invalid(p, ex) => p
    })

}