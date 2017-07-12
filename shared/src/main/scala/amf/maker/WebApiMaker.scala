package amf.maker

import amf.builder.BaseWebApiBuilder
import amf.model.BaseWebApi
import amf.parser.{AMFUnit, ASTNode}
import amf.spec.Spec

/**
  * Domain model WebApi Maker.
  */
class WebApiMaker(unit: AMFUnit) extends Maker[BaseWebApi](unit.vendor) {

  def matcher(builder: BaseWebApiBuilder, entry: ASTNode[_]): Unit = {
    Spec(vendor).fields.find(_.matcher.matches(entry)) match {
      case Some(field) => field.parse(field, entry, builder)
      case _           => // Unknown node...
    }
  }

  override def make: BaseWebApi = {
    val builder: BaseWebApiBuilder = builders.webApi
    val root                       = unit.root.children.head

    root.children.foreach(matcher(builder, _))

    builder
      .withProvider(OrganizationMaker(root, vendor).make)
      .withLicense(LicenseMaker(root, vendor).make)
      .withDocumentation(CreativeWorkMaker(root, vendor).make)
      .build
  }
}

object WebApiMaker {
  def apply(unit: AMFUnit): WebApiMaker = new WebApiMaker(unit)
}
