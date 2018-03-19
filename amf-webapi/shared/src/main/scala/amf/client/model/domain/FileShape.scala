package amf.client.model.domain

import amf.client.convert.WebApiClientConverters._
import amf.client.model.{IntField, StrField}
import amf.plugins.domain.shapes.models.{FileShape => InternalFileShape}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class FileShape(override private[amf] val _internal: InternalFileShape) extends AnyShape(_internal) {

  @JSExportTopLevel("model.domain.FileShape")
  def this() = this(InternalFileShape())

  def fileTypes: ClientList[StrField] = _internal.fileTypes.asClient
  def pattern: StrField               = _internal.pattern
  def minLength: IntField             = _internal.minLength
  def maxLength: IntField             = _internal.maxLength
  def minimum: StrField               = _internal.minimum
  def maximum: StrField               = _internal.maximum
  def exclusiveMinimum: StrField      = _internal.exclusiveMinimum
  def exclusiveMaximum: StrField      = _internal.exclusiveMaximum
  def format: StrField                = _internal.format
  def multipleOf: IntField            = _internal.multipleOf

  def withFileTypes(fileTypes: ClientList[String]): this.type = {
    _internal.withFileTypes(fileTypes.asInternal)
    this
  }
  def withPattern(pattern: String): this.type = {
    _internal.withPattern(pattern)
    this
  }
  def withMinLength(min: Int): this.type = {
    _internal.withMinLength(min)
    this
  }
  def withMaxLength(max: Int): this.type = {
    _internal.withMaxLength(max)
    this
  }
  def withMinimum(min: String): this.type = {
    _internal.withMinimum(min)
    this
  }
  def withMaximum(max: String): this.type = {
    _internal.withMaximum(max)
    this
  }
  def withExclusiveMinimum(min: String): this.type = {
    _internal.withExclusiveMinimum(min)
    this
  }
  def withExclusiveMaximum(max: String): this.type = {
    _internal.withExclusiveMaximum(max)
    this
  }
  def withFormat(format: String): this.type = {
    _internal.withFormat(format)
    this
  }
  def withMultipleOf(multiple: Int): this.type = {
    _internal.withMultipleOf(multiple)
    this
  }

  override def linkTarget: Option[FileShape] = _internal.linkTarget.map({ case l: InternalFileShape => l }).asClient

  override def linkCopy(): FileShape = _internal.linkCopy()
}