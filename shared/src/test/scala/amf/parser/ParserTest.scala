package amf.parser

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.yaml.model._
import org.yaml.parser.YamlParser

import scala.language.postfixOps

class ParserTest extends FunSuite {

  private val `RAML/yaml`: String =
    """a: 1
          |b: !include include1.yaml
          |c:
          |  - 2
          |  - 3
          |d: !include include2.yaml""".stripMargin

  private val `OAS/json`: String =
    """{
          |  "a": 1,
          |  "b": {
          |    "$ref": "include1.json"
          |  },
          |  "c": [
          |    2,
          |    3
          |  ],
          |  "d": {
          |    "$ref": "include2.json"
          |  }
          |}""".stripMargin

  private val `OAS/yaml`: String =
    """a: 1
          |b:
          |  $ref: include1.yaml
          |c:
          |  - 2
          |  - 3
          |d:
          |  $ref: include2.yaml""".stripMargin

  test("Test RAML/yaml") {
    val root = YamlParser(`RAML/yaml`).parse()
    root.size should be(1)

    val document = root.head.asInstanceOf[YDocument]
    document.value shouldBe defined
    document.value.get shouldBe a[YMap]

    assertDocumentRoot(document.value.get.asInstanceOf[YMap], assertRamlInclude)
  }

  test("Test OAS/json") {
    val root = YamlParser(`OAS/json`).parse(true)
    root.size should be(1)

    root.head shouldBe a[YDocument]
    val document = root.head.asInstanceOf[YDocument]
    document.value shouldBe defined
    document.value.get shouldBe a[YMap]

    assertDocumentRoot(document.value.get.asInstanceOf[YMap], assertOasInclude)
  }

  test("Test OAS/yaml") {
    val root = YamlParser(`OAS/yaml`).parse()
    root.size should be(1)

    val document = root.head.asInstanceOf[YDocument]
    document.value shouldBe defined
    document.value.get shouldBe a[YMap]

    assertDocumentRoot(document.value.get.asInstanceOf[YMap], assertOasInclude)
  }

  private def assertRamlInclude(entry: YMapEntry) = {
    entry.key.value shouldBe a[YScalar]
    Some(entry.key.value.asInstanceOf[YScalar].text) should contain oneOf ("b", "d")

    entry.value.value shouldBe a[YScalar]
    //todo parser: missing property for tag!
    entry.value.value.asInstanceOf[YScalar].text should startWith("include")
  }

  private def assertOasInclude(entry: YMapEntry) = {
    entry.key.value shouldBe a[YScalar]
    Some(entry.key.value.asInstanceOf[YScalar].text) should contain oneOf ("b", "d")

    entry.value.value shouldBe a[YMap]
    val include = entry.value.value.asInstanceOf[YMap].entries.head
    include.key.value shouldBe a[YScalar]
    include.key.value.asInstanceOf[YScalar].text shouldBe "$ref"
    include.value.value shouldBe a[YScalar]
    include.value.value.asInstanceOf[YScalar].text should startWith("include")
  }

  private def assertDocumentRoot(content: YMap, include: YMapEntry => Unit) = {
    content.entries.size should be(4)

    val first = content.entries(0)
    first.key.value shouldBe a[YScalar]
    first.key.value.asInstanceOf[YScalar].text shouldBe "a"
    first.value.value shouldBe a[YScalar]
    first.value.value.asInstanceOf[YScalar].text shouldBe "1"

    include(content.entries(1))

    val third = content.entries(2)
    third.key.value shouldBe a[YScalar]
    third.key.value.asInstanceOf[YScalar].text shouldBe "c"
    third.value.value shouldBe a[YSequence]

    val sequence = third.value.value.asInstanceOf[YSequence]
    sequence.values.size should be(2)
    sequence.values(0) shouldBe a[YScalar]
    sequence.values(0).asInstanceOf[YScalar].text shouldBe "2"
    sequence.values(1) shouldBe a[YScalar]
    sequence.values(1).asInstanceOf[YScalar].text shouldBe "3"

    include(content.entries(3))
  }
}
