package amf.emit

import amf.core.annotations.SynthesizedField
import amf.core.model.document.{Document, Module}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.domain.shapes.models.CreativeWork
import amf.plugins.domain.webapi.models.{License, Organization, WebApi}

/**
  *
  */
trait AMFUnitFixtureTest extends PlatformSecrets {

  def `document/api/bare`: Document = doc(bare())

  def `document/api/basic`: Document = doc(basic())

  def `document/api/advanced`: Document = doc(advanced())

  def `document/api/full`: Document = doc(advanced())

  def `module/bare`: Module = libraryBare()

  private def bare(): WebApi = {
    val api = WebApi()
      .withName("test")
      .withDescription("test description")
      .withSchemes(List("http", "https"))
      .withAccepts(List("application/json"))
      .withContentType(List("application/json"))
      .withVersion("1.1")
      .withTermsOfService("termsOfService")
      .adopted("file:///tmp/test")

    api.withServer("localhost.com/api").add(SynthesizedField())

    api
  }

  private def basic(): WebApi = {
    val api = bare()
    api
      .withProvider(
        Organization()
          .withEmail("test@test")
          .withName("organizationName")
          .withUrl("organizationUrl")
      )
      .withLicense(
        License()
          .withName("licenseName")
          .withUrl("licenseUrl")
      )
      .withDocumentationUrl("creativoWorkUrl")
      .withDescription("creativeWorkDescription")

    api
  }

  private def advanced(): WebApi = {
    val api = basic()

    val endpoint = api
      .withEndPoint("/endpoint")
      .withDescription("test endpoint")
      .withName("endpoint")

    endpoint
      .withOperation("get")
      .withDescription("test operation get")
      .withName("test get")
      .withSchemes(List("http"))
      .withSummary("summary of operation get")
      .withDocumentation(
        CreativeWork()
          .withDescription("documentation operation")
          .withUrl("localhost:8080/endpoint/operation")
      )

    endpoint
      .withOperation("post")
      .withDescription("test operation post")
      .withDeprecated(true)
      .withName("test post")
      .withSchemes(List("http"))
      .withSummary("summary of operation post")
      .withDocumentation(
        CreativeWork()
          .withDescription("documentation operation")
          .withUrl("localhost:8080/endpoint/operation")
      )

    api
  }

  def libraryBare(): Module = {
    Module()
      .withId("file://amf-client/shared/src/test/resources/clients/lib/lib.raml")
      .withLocation("file://amf-client/shared/src/test/resources/clients/lib/lib.raml")
      .withUsage("Data types and annotation types")

  }

  private def doc(api: WebApi): Document =
    Document().withLocation("file:///tmp/test").adopted("file:///tmp/test").withEncodes(api)
}
