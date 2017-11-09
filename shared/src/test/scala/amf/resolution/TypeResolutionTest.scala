package amf.resolution

import amf.ProfileNames
import amf.client.GenerationOptions
import amf.compiler.AMFCompiler
import amf.document.BaseUnit
import amf.dumper.AMFDumper
import amf.io.BuildCycleTests
import amf.remote.Syntax.Yaml
import amf.remote.{Raml, RamlYamlHint}
import amf.shape._
import amf.spec.{Declarations, ParserContext}
import amf.spec.raml.RamlTypeExpressionParser
import amf.validation.Validation
import amf.vocabulary.Namespace
import org.scalatest.Assertion

import scala.concurrent.Promise

class TypeResolutionTest extends BuildCycleTests {

  test("TypeExpressions") {

    val adopt = (shape: Shape) => { shape.adopted("/test") }

    implicit val ctx = ParserContext(Validation(platform), Raml)

    var res = RamlTypeExpressionParser(adopt, Declarations()).parse("integer")
    assert(res.get.isInstanceOf[ScalarShape])
    assert(res.get.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer)")
    assert(res.get.isInstanceOf[ScalarShape])
    assert(res.get.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("((integer))")
    assert(res.get.isInstanceOf[ScalarShape])
    assert(res.get.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("integer[]")
    assert(res.get.isInstanceOf[ArrayShape])
    assert(
      res.get.asInstanceOf[ArrayShape].items.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())
    assert(res != null)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer)[]")
    assert(res.get.isInstanceOf[ArrayShape])
    assert(
      res.get.asInstanceOf[ArrayShape].items.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())
    assert(res != null)

    var error = false
    try {
      implicit val fail = ParserContext(Validation(platform).withEnabledValidation(false), Raml)
      val parser        = RamlTypeExpressionParser(adopt, Declarations())(fail)
      parser.parse("[]")
    } catch {
      case e: Exception => error = true
    }
    assert(error)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("integer | string")
    assert(res.get.isInstanceOf[UnionShape])
    var union = res.get.asInstanceOf[UnionShape]
    assert(union.anyOf.length == 2)
    assert(union.anyOf.map { e =>
      e.asInstanceOf[ScalarShape].dataType
    } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
    assert(res != null)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer )| (string)")
    assert(res.get.isInstanceOf[UnionShape])
    union = res.get.asInstanceOf[UnionShape]
    assert(union.anyOf.length == 2)
    assert(union.anyOf.map { e =>
      e.asInstanceOf[ScalarShape].dataType
    } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
    assert(res != null)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer | string) | number")
    assert(res.get.isInstanceOf[UnionShape])
    union = res.get.asInstanceOf[UnionShape]
    assert(union.anyOf.length == 3)
    assert(union.anyOf.map { e =>
      e.asInstanceOf[ScalarShape].dataType
    } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri(), (Namespace.Xsd + "float").iri()))
    assert(res != null)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer | string)[]")
    assert(res.get.isInstanceOf[ArrayShape])
    var array = res.get.asInstanceOf[ArrayShape]
    assert(array.items.isInstanceOf[UnionShape])
    union = array.items.asInstanceOf[UnionShape]
    assert(union.anyOf.map { e =>
      e.asInstanceOf[ScalarShape].dataType
    } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
    assert(res != null)

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("(integer | string[])")
    assert(res != null)
    assert(res.get.isInstanceOf[UnionShape])
    union = res.get.asInstanceOf[UnionShape]
    assert(union.anyOf.length == 2)
    assert(union.anyOf.head.isInstanceOf[ScalarShape])
    assert(union.anyOf.head.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())
    assert(union.anyOf.last.isInstanceOf[ArrayShape])
    assert(
      union.anyOf.last.asInstanceOf[ArrayShape].items.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "string")
        .iri())

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("integer | string[]")
    assert(res != null)
    assert(res.get.isInstanceOf[UnionShape])
    union = res.get.asInstanceOf[UnionShape]
    assert(union.anyOf.length == 2)
    assert(union.anyOf.head.isInstanceOf[ScalarShape])
    assert(union.anyOf.head.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())
    assert(union.anyOf.last.isInstanceOf[ArrayShape])
    assert(
      union.anyOf.last.asInstanceOf[ArrayShape].items.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "string")
        .iri())

    res = RamlTypeExpressionParser(adopt, Declarations()).parse("integer[][]")
    assert(res != null)
    assert(res.get.isInstanceOf[MatrixShape])
    var matrix = res.get.asInstanceOf[MatrixShape]
    assert(matrix.items.isInstanceOf[ArrayShape])
    array = matrix.items.asInstanceOf[ArrayShape]
    assert(array.items.isInstanceOf[ScalarShape])
    assert(array.items.asInstanceOf[ScalarShape].dataType == (Namespace.Xsd + "integer").iri())
  }

  override val basePath = "file://shared/src/test/resources/resolution/"

  val examples = Seq(
    "union1",
    "union2",
    "union3",
    "union4",
    "union5",
    "inheritance1",
    "inheritance2",
    "inheritance3",
    "inheritance4",
    "inheritance5",
    "inheritance6",
    "inheritance7",
    "inheritance8",
    "array_inheritance1",
    "array_inheritance2",
    "array_inheritance3",
    "complex_example1",
    "shape1",
    "shape2",
    "shape3"
  )

  examples.foreach { example =>
    test(s"Resolve data types: $example") {
      val validation = Validation(platform)
      validation.loadValidationDialect() flatMap { _ =>
        cycle(s"$example.raml", s"${example}_canonical.raml", RamlYamlHint, Raml, basePath, Some(validation))
      }
    }
  }

  override def map(unit: BaseUnit, config: CycleConfig): BaseUnit = unit.resolve(ProfileNames.RAML)

  val errorExamples = Seq(
    "inheritance_error1",
    "inheritance_error2",
    "inheritance_error3"
  )

  errorExamples.foreach { example =>
    test(s"Fails on erroneous data types: $example") {
      val res = AMFCompiler(basePath + s"$example.raml",
                            platform,
                            RamlYamlHint,
                            Validation(platform),
                            None,
                            None,
                            platform.dialectsRegistry)
        .build()
        .map { model =>
          model.resolve(ProfileNames.RAML)
        }

      val p = Promise[Assertion]()
      res.onComplete { res =>
        if (res.isSuccess) {
          AMFDumper(res.get, Raml, Yaml, GenerationOptions()).dumpToString.map { doc =>
            p.success(assert(res.isFailure))
          }
        } else {
          p.success(assert(res.isFailure))
        }
      }
      p.future
    }
  }
}
