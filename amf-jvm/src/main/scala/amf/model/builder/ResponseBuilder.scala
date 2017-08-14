package amf.model.builder

import amf.model.{Parameter, Payload, Response}

import scala.collection.JavaConverters._

/**
  * Response builder.
  */
case class ResponseBuilder private (
    private val internalBuilder: amf.builder.ResponseBuilder = amf.builder.ResponseBuilder())
    extends Builder {

  def this() = this(amf.builder.ResponseBuilder())

  def withName(name: String): ResponseBuilder = {
    internalBuilder.withName(name)
    this
  }

  def withDescription(description: String): ResponseBuilder = {
    internalBuilder.withDescription(description)
    this
  }

  def withStatusCode(statusCode: String): ResponseBuilder = {
    internalBuilder.withStatusCode(statusCode)
    this
  }

  def withHeaders(headers: java.util.List[Parameter]): ResponseBuilder = {
    internalBuilder.withHeaders(headers.asScala.map(_.element).toList)
    this
  }

  def withPayloads(payloads: java.util.List[Payload]): ResponseBuilder = {
    internalBuilder.withPayloads(payloads.asScala.map(_.element).toList)
    this
  }

  def build: Response = Response(internalBuilder.build)
}
