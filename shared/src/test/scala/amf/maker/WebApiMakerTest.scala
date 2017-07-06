package amf.maker

import amf.common.ListAssertions
import amf.compiler.AMFCompiler
import amf.metadata.Field
import amf.metadata.model.WebApiModel._
import amf.model.{BaseWebApi, CreativeWork, License, Organization}
import amf.parser._
import amf.remote.{Hint, OasJsonHint, RamlYamlHint}
import amf.unsafe.PlatformSecrets
import org.scalatest.Matchers._
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.{ExecutionContext, Future}

class WebApiMakerTest extends AsyncFunSuite with PlatformSecrets with ListAssertions {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "file://shared/src/test/resources/maker/"

  test("generate complete web api instance") {

    val fixture = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContacto", "nombreContacto", "mailContacto")),
      (License, new License("urlLicense", "nameLicense")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    assertFixture(fixture, "completeExample.raml", Some(RamlYamlHint))
  }

  test("generate partial succeed") {
    val fixture = List(
      (Name, "test"),
      (Description, null),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, null),
      (Provider, Organization("urlContacto", "nombreContacto", "mailContacto")),
      (License, null),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    assertFixture(fixture, "partialExample.raml", Some(RamlYamlHint))
  }

  test("basic jsonld example") {
    val fixture = List(
      (Name, "test"),
      (Description, null),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, null),
      (TermsOfService, null),
      (Provider, null),
      (License, null),
      (Documentation, null)
    )

    assertFixture(fixture, "basicExample.jsonld", Some(RamlYamlHint))
  }

  test("generate partial json") {

    val fixture = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContact", "nameContact", "emailContact")),
      (License, new License("urlLicense", "nameLicense")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    assertFixture(fixture, "completeExample.json", Some(RamlYamlHint))
  }

  test("edit complete raml") {

    val before = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContacto", "nombreContacto", "mailContacto")),
      (License, new License("urlLicense", "nameLicense")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    val after = List(
      (Name, "test"),
      (Description, "changed"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContacto", "nombreContacto", "mailContacto")),
      (License, new License("urlLicense", "changed")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    AMFCompiler(basePath + "completeExample.raml", platform, Some(RamlYamlHint))
      .build()
      .map {
        case (root, vendor) => WebApiMaker(AMFUnit(root, basePath + "completeExample.raml", Document, vendor)).make
      }
      .map { api =>
        assertWebApiValues(api, before)
        val builder = api.toBuilder
        builder.withDescription("changed")
        val license = api.license.toBuilder
        license.withName("changed")
        builder.withLicense(license.build)
        assertWebApiValues(builder.build, after)
      }
  }

  test("edit complete json") {
    val before = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContact", "nameContact", "emailContact")),
      (License, new License("urlLicense", "nameLicense")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    val after = List(
      (Name, "test"),
      (Description, "changed"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, Organization("urlContact", "nameContact", "changed")),
      (License, new License("urlLicense", "nameLicense")),
      (Documentation, CreativeWork("urlExternalDocs", "descriptionExternalDocs"))
    )

    AMFCompiler(basePath + "completeExample.json", platform, Some(OasJsonHint))
      .build()
      .map {
        case (root, vendor) => WebApiMaker(AMFUnit(root, basePath + "completeExample.json", Document, vendor)).make
      }
      .map { api =>
        assertWebApiValues(api, before)
        val builder = api.toBuilder
        builder.withDescription("changed")
        val license = api.license.toBuilder
        license.withName("changed")
        builder.withLicense(license.build)
        assertWebApiValues(builder.build, after)
      }
  }

  def assertWebApiValues(api: BaseWebApi, assertions: List[(Field, Any)]): Assertion = {
    assertions.foreach {
      case (Name, expected)           => assertField(Name, api.name, expected)
      case (Description, expected)    => assertField(Description, api.description, expected)
      case (Host, expected)           => assertField(Host, api.host, expected)
      case (Schemes, expected)        => assertField(Schemes, api.schemes, expected)
      case (BasePath, expected)       => assertField(BasePath, api.basePath, expected)
      case (ContentType, expected)    => assertField(ContentType, api.contentType, expected)
      case (Accepts, expected)        => assertField(Accepts, api.accepts, expected)
      case (Version, expected)        => assertField(Version, api.version, expected)
      case (TermsOfService, expected) => assertField(TermsOfService, api.termsOfService, expected)
      case (Provider, expected)       => assertField(Provider, api.provider, expected)
      case (License, expected)        => assertField(License, api.license, expected)
      case (Documentation, expected)  => assertField(Documentation, api.documentation, expected)
    }
    succeed
  }

  private def assertField(field: Field, actual: Any, expected: Any) =
    if (expected != actual) fail(s"Expected $expected but $actual found for field ${field.name}")

  private def assertFixture(fixture: List[(Field, Object)], file: String, hint: Some[Hint]): Future[Assertion] = {
    AMFCompiler(basePath + file, platform, hint)
      .build()
      .map {
        case (root, vendor) => WebApiMaker(AMFUnit(root, basePath + file, Document, vendor)).make
      }
      .map {
        assertWebApiValues(_, fixture)
      }
  }
}
