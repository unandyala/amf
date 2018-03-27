package amf.facades

import amf.Core
import amf.core.emitter.RenderOptions
import amf.core.AMFSerializer
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.Syntax
import amf.core.remote._
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.RAMLVocabulariesPlugin
import amf.plugins.document.webapi.{OAS20Plugin, PayloadPlugin, RAML08Plugin, RAML10Plugin, _}
import amf.plugins.domain.shapes.DataShapesDomainPlugin
import amf.plugins.domain.webapi.WebAPIDomainPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

// TODO: this is only here for compatibility with the test suite
class AMFRenderer(unit: BaseUnit, vendor: Vendor, syntax: Syntax, options: RenderOptions) {

  Core.init()
  amf.core.registries.AMFPluginsRegistry.registerSyntaxPlugin(SYamlSyntaxPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(RAML10Plugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(RAML08Plugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(OAS20Plugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(OAS30Plugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(PayloadPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(AMFGraphPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(JsonSchemaPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(RAMLVocabulariesPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDomainPlugin(WebAPIDomainPlugin)
  amf.core.registries.AMFPluginsRegistry.registerDomainPlugin(DataShapesDomainPlugin)

  /** Print ast to string. */
  def renderToString: Future[String] = render()

  /** Print ast to file. */
  def renderToFile(remote: Platform, path: String): Future[Unit] = render().flatMap(s => remote.write(path, s))

  private def render(): Future[String] = {
    val vendorString = vendor match {
      case RamlVocabulary => "RAML Vocabularies"
      case Amf            => "AMF Graph"
      case Payload        => "AMF Payload"
      case Raml08         => "RAML 0.8"
      case Raml10 | Raml  => "RAML 1.0"
      case Oas            => "OAS 2.0"
      case Oas2           => "OAS 2.0"
      case Oas3           => "OAS 3.0"
      case Extension      => "RAML Vocabularies"
      case _              => "Unknown Vendor"
    }

    val mediaType = vendor match {
      case Amf                                     => "application/ld+json"
      case Payload                                 => "application/amf+json"
      case Raml10 | Raml08 | Raml | RamlVocabulary => "application/yaml"
      case Oas | Oas2 | Oas3                       => "application/json"
      case Extension                               => "application/yaml"
      case _                                       => "text/plain"
    }

    new AMFSerializer(unit, mediaType, vendorString, options).renderToString
  }
}

object AMFRenderer {
  def apply(unit: BaseUnit, vendor: Vendor, syntax: Syntax, options: RenderOptions): AMFRenderer =
    new AMFRenderer(unit, vendor, syntax, options)
}