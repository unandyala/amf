package amf.plugins.document.webapi.contexts

import amf.core.model.document.{ExternalFragment, RecursiveUnit}
import amf.core.model.domain.Shape
import amf.core.parser.{ParsedReference, ParserContext, YMapOps}
import amf.core.remote._
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.webapi.JsonSchemaPlugin
import amf.plugins.document.webapi.parser.spec.oas.{Oas2Syntax, Oas3Syntax}
import amf.plugins.document.webapi.parser.spec.raml.{Raml08Syntax, Raml10Syntax}
import amf.plugins.document.webapi.parser.spec.{RamlWebApiDeclarations, SpecSyntax, WebApiDeclarations}
import amf.plugins.domain.shapes.models.AnyShape
import amf.plugins.features.validation.ParserSideValidations.{
  ClosedShapeSpecification,
  DuplicatedPropertySpecification
}
import org.yaml.model._

class PayloadContext(loc: String,
                     refs: Seq[ParsedReference],
                     private val wrapped: ParserContext,
                     private val ds: Option[RamlWebApiDeclarations] = None)
    extends RamlWebApiContext(loc, refs, wrapped, ds) {
  override protected def clone(declarations: RamlWebApiDeclarations): RamlWebApiContext = {
    new PayloadContext(loc, refs, wrapped, Some(declarations))
  }
  override val factory: RamlSpecVersionFactory = new Raml10VersionFactory()(this)
  override val syntax: SpecSyntax = new SpecSyntax {
    override val nodes: Map[String, Set[String]] = Map()
  }
  override val vendor: Vendor = Payload
}

class Raml10WebApiContext(loc: String,
                          refs: Seq[ParsedReference],
                          private val wrapped: ParserContext,
                          private val ds: Option[RamlWebApiDeclarations] = None)
    extends RamlWebApiContext(loc, refs, wrapped, ds) {
  override val factory: RamlSpecVersionFactory = new Raml10VersionFactory()(this)
  override val vendor: Vendor                  = Raml10
  override val syntax: SpecSyntax              = Raml10Syntax

  override protected def clone(declarations: RamlWebApiDeclarations): RamlWebApiContext =
    new Raml10WebApiContext(loc, refs, wrapped, Some(declarations))
}

class Raml08WebApiContext(loc: String,
                          refs: Seq[ParsedReference],
                          private val wrapped: ParserContext,
                          private val ds: Option[RamlWebApiDeclarations] = None)
    extends RamlWebApiContext(loc, refs, wrapped, ds) {
  override val factory: RamlSpecVersionFactory = new Raml08VersionFactory()(this)
  override val vendor: Vendor                  = Raml08
  override val syntax: SpecSyntax              = Raml08Syntax

  override protected def clone(declarations: RamlWebApiDeclarations): RamlWebApiContext =
    new Raml08WebApiContext(loc, refs, wrapped, Some(declarations))
}

abstract class RamlWebApiContext(loc: String,
                                 refs: Seq[ParsedReference],
                                 private val wrapped: ParserContext,
                                 private val ds: Option[RamlWebApiDeclarations] = None)
    extends WebApiContext(loc, refs, wrapped, ds)
    with RamlSpecAwareContext {

  var globalMediatype: Boolean = false

  override val declarations: RamlWebApiDeclarations = ds.getOrElse(
    new RamlWebApiDeclarations(alias = None, errorHandler = Some(this), futureDeclarations = futureDeclarations))
  protected def clone(declarations: RamlWebApiDeclarations): RamlWebApiContext

  /**
    * Adapt this context for a nested library, used when evaluating resource type / traits
    * Using a path to the library whose context is going to be looked up, e.g. lib.TypeA
    */
  def adapt[T](path: String)(k: RamlWebApiContext => T): T = {
    val pathElements        = path.split("\\.").dropRight(1)
    val adaptedDeclarations = findDeclarations(pathElements, declarations)
    k(clone(declarations.merge(adaptedDeclarations)))
  }

  protected def findDeclarations(path: Seq[String], declarations: RamlWebApiDeclarations): RamlWebApiDeclarations = {
    if (path.isEmpty) {
      declarations
    } else {
      val nextLibrary = path.head
      declarations.libraries.get(nextLibrary) match {
        case Some(library: RamlWebApiDeclarations) =>
          findDeclarations(path.tail, library)
        case _ => throw new Exception(s"Cannot find declarations in context '${path.mkString(".")}")
      }
    }
  }

  override def link(node: YNode): Either[String, YNode] = {
    node match {
      case _ if isInclude(node) => Left(node.as[YScalar].text)
      case _                    => Right(node)
    }
  }

  override def ignore(shape: String, property: String): Boolean =
    (property.startsWith("(") && property.endsWith(")")) || (property.startsWith("/") && (shape == "webApi" || shape == "endPoint"))

  private def isInclude(node: YNode) = node.tagType == YType.Include

  val factory: RamlSpecVersionFactory

  /**
    * raml types nodes are different from other shapes because they can have 'custom facets' essentially, client
    * defined constraints expressed as additional properties syntactically in the type definition.
    * The problem is that they cannot be recognised just looking into the AST as we do with annotations, so we
    * need to first, compute them, and then, add them as additional valid properties to the set of properties that
    * can be defined in the AST node
    */
  def closedRamlTypeShape(shape: Shape, ast: YMap, shapeType: String, annotation: Boolean = false): Unit = {
    val node   = shape.id
    val facets = shape.collectCustomShapePropertyDefinitions(onlyInherited = true)

    syntax.nodes.get(shapeType) match {
      case Some(props) =>
        val initialProperties = if (annotation) {
          props ++ syntax.nodes("annotation")
        } else {
          props
        }
        val allResults: Seq[Seq[YMapEntry]] = facets.map { propertiesMap =>
          val totalProperties     = initialProperties ++ propertiesMap.keys.toSet
          val acc: Seq[YMapEntry] = Seq.empty
          ast.entries.foldLeft(acc) { (results: Seq[YMapEntry], entry) =>
            val key: String = entry.key.as[YScalar].text
            if (ignore(shapeType, key)) {
              results
            } else if (!totalProperties(key)) {
              results ++ Seq(entry)
            } else {
              results
            }
          }
        }
        allResults.find(_.nonEmpty) match {
          case None => // at least we found a solution, this is a valid shape
          case Some(errors: Seq[YMapEntry]) =>
            violation(
              ClosedShapeSpecification.id(),
              node,
              s"Properties ${errors.map(_.key.as[YScalar].text).mkString(",")} not supported in a $vendor $shapeType node",
              errors.head
            ) // pointing only to the first failed error
        }

      case None => throw new Exception(s"Cannot validate unknown node type $shapeType for $vendor")
    }
  }
}

abstract class OasWebApiContext(loc: String,
                                refs: Seq[ParsedReference],
                                private val wrapped: ParserContext,
                                private val ds: Option[WebApiDeclarations] = None)
    extends WebApiContext(loc, refs, wrapped, ds) {

  val factory: OasSpecVersionFactory

  override def link(node: YNode): Either[String, YNode] = {
    node.to[YMap] match {
      case Right(map) =>
        val ref: Option[String] = map.key("$ref").flatMap(v => v.value.asOption[YScalar]).map(_.text)
        ref match {
          case Some(url) => Left(url)
          case None      => Right(node)
        }
      case _ => Right(node)
    }
  }

  override def ignore(shape: String, property: String): Boolean =
    property.startsWith("x-") || property == "$ref" || (property.startsWith("/") && shape == "webApi")
}

class Oas2WebApiContext(loc: String,
                        refs: Seq[ParsedReference],
                        private val wrapped: ParserContext,
                        private val ds: Option[WebApiDeclarations] = None)
    extends OasWebApiContext(loc, refs, wrapped, ds) {
  override val factory: Oas2VersionFactory = Oas2VersionFactory()(this)
  override val vendor: Vendor              = Oas2
  override val syntax: SpecSyntax          = Oas2Syntax
}

class Oas3WebApiContext(loc: String,
                        refs: Seq[ParsedReference],
                        private val wrapped: ParserContext,
                        private val ds: Option[WebApiDeclarations] = None)
    extends OasWebApiContext(loc, refs, wrapped, ds) {
  override val factory: Oas3VersionFactory = Oas3VersionFactory()(this)
  override val vendor: Vendor              = Oas3
  override val syntax: SpecSyntax          = Oas3Syntax
}

abstract class WebApiContext(loc: String,
                             refs: Seq[ParsedReference],
                             private val wrapped: ParserContext,
                             private val ds: Option[WebApiDeclarations] = None)
    extends ParserContext(loc, refs, wrapped.futureDeclarations, parserCount = wrapped.parserCount)
    with SpecAwareContext
    with PlatformSecrets {

  val syntax: SpecSyntax
  val vendor: Vendor

  val declarations: WebApiDeclarations =
    ds.getOrElse(new WebApiDeclarations(None, errorHandler = Some(this), futureDeclarations = futureDeclarations))

  var localJSONSchemaContext: Option[YNode] = wrapped match {
    case (wac: WebApiContext) => wac.localJSONSchemaContext
    case _                    => None
  }

  globalSpace = wrapped.globalSpace

  // JSON Schema has a global namespace
  def findJsonSchema(url: String): Option[AnyShape] = globalSpace.get(url) match {
    case Some(shape: AnyShape) => Some(shape)
    case _                     => None
  }
  def registerJsonSchema(url: String, shape: AnyShape) = globalSpace.update(url, shape)

  def parseRemoteJSONPath(fileUrl: String)(implicit ctx: OasWebApiContext): Option[AnyShape] = {
    val referenceUrl = if (fileUrl.contains("#")) Some(fileUrl.split("#").last) else None
    val baseFileUrl  = fileUrl.split("#").head
    val res: Option[Option[AnyShape]] = refs
      .filter(r => Option(r.unit.location).isDefined)
      .filter(_.unit.location == baseFileUrl) collectFirst {
      case ref if ref.unit.isInstanceOf[ExternalFragment] =>
        val jsonFile = ref.unit.asInstanceOf[ExternalFragment]
        JsonSchemaPlugin.parseFragment(jsonFile, referenceUrl)
      case ref if ref.unit.isInstanceOf[RecursiveUnit] =>
        val jsonFile = ref.unit.asInstanceOf[RecursiveUnit]
        JsonSchemaPlugin.parseFragment(jsonFile, referenceUrl)
    }
    res.flatten
  }

  def resolvedPath(base: String, str: String): String = {
    if (str.startsWith("/")) str
    else if (str.contains("://")) str
    else if (str.startsWith("#")) base.split("#").head + str
    else basePath(base) + str
  }

  def basePath(path: String): String = {
    val withoutHash = if (path.contains("#")) path.split("#").head else path
    withoutHash.splitAt(withoutHash.lastIndexOf("/"))._1 + "/"
  }

  def findLocalJSONPath(path: String): Option[(String, YNode)] = {
    localJSONSchemaContext match {
      case None => None
      case Some(schema) =>
        var tmp: YNode = schema
        var name       = "schema"
        var parts      = path.replace("#", "").split("/").filter(_ != "")
        var error      = false

        while (parts.nonEmpty && !error) {
          tmp.tagType match {
            case YType.Map =>
              val nextPart = parts.head
              parts = parts.tail
              val map = tmp.as[YMap]
              map.key(nextPart) match {
                case Some(entry) =>
                  name = nextPart
                  tmp = entry.value
                case _ =>
                  error = true
              }
            case YType.Seq =>
              val nextPart = parts.head
              parts = parts.tail
              val seq = tmp.as[YSequence]
              if (nextPart.matches("[0-9]+")) {
                val pos = Integer.parseInt(nextPart)
                if (seq.nodes.size <= pos) {
                  error = true
                } else {
                  tmp = seq.nodes(pos)
                }
              } else {
                error = true
              }
            case _ =>
              error = true
          }
        }

        if (!error) Some((name, tmp)) else None
    }
  }

  def link(node: YNode): Either[String, YNode]
  def ignore(shape: String, property: String): Boolean

  def checkDuplicates(node: String, ast: YMap, shape: String, annotation: Boolean) = {
    ast.entries.foldLeft(Set[String]()) {
      case (acc, entry) =>
        acc.contains(entry.key.toString()) match {
          case true =>
            violation(DuplicatedPropertySpecification.id(), node, s"Property '${entry.key}' is duplicated", entry)
            acc
          case false =>
            acc ++ Set(entry.key.toString())
        }
    }
  }

  /** Validate closed shape. */
  def closedShape(node: String, ast: YMap, shape: String, annotation: Boolean = false): Unit = {
    checkDuplicates(node, ast, shape, annotation)
    syntax.nodes.get(shape) match {
      case Some(props) =>
        val properties = if (annotation) {
          props ++ syntax.nodes("annotation")
        } else {
          props
        }

        ast.entries.foreach { entry =>
          val key: String = entry.key.as[YScalar].text
          if (ignore(shape, key)) {
            // annotation or path in endpoint/webapi => ignore
          } else if (!properties(key)) {
            violation(ClosedShapeSpecification.id(),
                      node,
                      s"Property $key not supported in a $vendor $shape node",
                      entry)
          }
        }
      case None => throw new Exception(s"Cannot validate unknown node type $shape for $vendor")
    }
  }
}
