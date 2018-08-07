package amf.plugins.syntax

import java.io.Writer

import amf.client.plugins.{AMFPlugin, AMFSyntaxPlugin}
import amf.core.benchmark.ExecutionLog
import amf.core.parser.{ParsedDocument, ParserContext, SyamlParsedDocument}
import amf.core.unsafe.PlatformSecrets
import org.yaml.model.{YComment, YDocument, YMap, YNode}
import org.yaml.parser.{JsonParser, YamlParser}
import org.yaml.render.{JsonRender, YamlRender}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SYamlSyntaxPlugin extends AMFSyntaxPlugin with PlatformSecrets {

  override val ID = "SYaml"

  override def init(): Future[AMFPlugin] = Future { this }

  override def dependencies(): Seq[AMFPlugin] = Nil

  override def supportedMediaTypes() = Seq(
    "application/yaml",
    "application/x-yaml",
    "text/yaml",
    "text/x-yaml",
    "application/json",
    "text/json",
    "application/raml",
    "text/vnd.yaml"
  )

  override def parse(mediaType: String, vendor: String, text: CharSequence, ctx: ParserContext): Option[ParsedDocument] = {

    if (mediaType == "application/ld+json" && vendor == "AML 1.0" && platform.rdfFramework.isDefined) { // TODO: remove the hard-coded AML 1.0 somehow!
      platform.rdfFramework.get.syntaxToRdfModel(mediaType, text)
    } else {
      val parser = getFormat(mediaType) match {
        case "json" => JsonParser.withSource(text, ctx.currentFile)(ctx).withIncludeTag("!include")
        case _      => YamlParser(text, ctx.currentFile)(ctx).withIncludeTag("!include")
      }
      val parts = parser.parse(true)

      if (parts.exists(v => v.isInstanceOf[YDocument])) {
        parts collectFirst { case d: YDocument => d } map { document =>
          val comment = parts collectFirst { case c: YComment => c }
          SyamlParsedDocument(comment, document)
        }
      } else {
        parts collectFirst { case d: YComment => d } map { comment =>
          SyamlParsedDocument(Some(comment), YDocument(IndexedSeq(YNode(YMap.empty)), ctx.currentFile))
        }
      }
    }
  }

  override def unparse(mediaType: String, ast: YDocument): Some[String] = render(mediaType, ast) { (format, ast) =>
    Some(if (format == "yaml") YamlRender.render(ast) else JsonRender.render(ast))
  }

  override def unparse(mediaType: String, ast: YDocument, writer: Writer): Option[Writer] = render(mediaType, ast) {
    (format, ast) =>
      Some(if (format == "yaml") writer.append(YamlRender.render(ast)) else JsonRender.render(ast, writer))
  }

  private def render[T](mediaType: String, ast: YDocument)(render: (String, YDocument) => T): T = {
    val format = getFormat(mediaType)

    ExecutionLog.log(s"Serialising to format $format")

    val result: T = render(format, ast)

    ExecutionLog.log(s"Got the serialisation $format")

    result
  }

  private def getFormat(mediaType: String) = if (mediaType.contains("json")) "json" else "yaml"
}
