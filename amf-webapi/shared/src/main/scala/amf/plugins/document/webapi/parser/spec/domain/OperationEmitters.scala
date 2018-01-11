package amf.plugins.document.webapi.parser.spec.domain

import amf.core.emitter.BaseEmitters._
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.document.BaseUnit
import amf.core.parser.{Fields, Position}
import amf.plugins.document.webapi.contexts.RamlSpecEmitterContext
import amf.plugins.document.webapi.parser.spec.declaration._
import amf.plugins.domain.shapes.models.{AnyShape, CreativeWork}
import amf.plugins.domain.webapi.metamodel.{OperationModel, RequestModel}
import amf.plugins.domain.webapi.models.Operation
import org.yaml.model.YDocument.EntryBuilder

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  *
  */
case class Raml10OperationEmitter(operation: Operation, ordering: SpecOrdering, references: Seq[BaseUnit])(
    implicit spec: RamlSpecEmitterContext)
    extends RamlOperationEmitter(operation, ordering, references) {

  override protected def entries(fs: Fields): Seq[EntryEmitter] = {
    val emitters = super.entries(fs)
    val results  = ListBuffer[EntryEmitter]()
    fs.entry(OperationModel.Description).map(f => results += ValueEmitter("description", f))

    Option(operation.request).foreach { req =>
      req.fields
        .entry(RequestModel.QueryString)
        .map { f =>
          Option(f.value.value) match {
            case Some(shape: AnyShape) =>
              results += RamlNamedTypeEmitter(shape, ordering, references, Raml10TypePartEmitter.apply)
            case Some(_) => throw new Exception("Cannot emit non WebApi Shape")
            case _       => // ignore
          }

        }
    }

    results ++ emitters ++ AnnotationsEmitter(operation, ordering).emitters

  }

  override protected val baseUriParameterKey: String = "(baseUriParameters)"
}

case class Raml08OperationEmitter(operation: Operation, ordering: SpecOrdering, references: Seq[BaseUnit])(
    implicit spec: RamlSpecEmitterContext)
    extends RamlOperationEmitter(operation, ordering, references) {

  override protected val baseUriParameterKey: String = "baseUriParameters"
}

abstract class RamlOperationEmitter(operation: Operation, ordering: SpecOrdering, references: Seq[BaseUnit])(
    implicit spec: RamlSpecEmitterContext)
    extends EntryEmitter {

  protected val baseUriParameterKey: String

  protected def entries(fs: Fields): Seq[EntryEmitter] = {
    val result = mutable.ListBuffer[EntryEmitter]()

    fs.entry(OperationModel.Name).map(f => result += ValueEmitter("displayName", f))

    fs.entry(OperationModel.Deprecated).map(f => result += ValueEmitter("(deprecated)", f))

    fs.entry(OperationModel.Summary).map(f => result += ValueEmitter("(summary)", f))

    fs.entry(OperationModel.Documentation)
      .map(f =>
        result += OasEntryCreativeWorkEmitter("(externalDocs)", f.value.value.asInstanceOf[CreativeWork], ordering))

    fs.entry(OperationModel.Schemes).map(f => result += ArrayEmitter("protocols", f, ordering))

    fs.entry(OperationModel.Accepts).map(f => result += ArrayEmitter("(consumes)", f, ordering))

    fs.entry(OperationModel.ContentType).map(f => result += ArrayEmitter("(produces)", f, ordering))

    fs.entry(DomainElementModel.Extends).map(f => result ++= ExtendsEmitter("", f, ordering).emitters())

    Option(operation.request).foreach { req =>
      val fields = req.fields

      fields
        .entry(RequestModel.QueryParameters)
        .map(f => result += RamlParametersEmitter("queryParameters", f, ordering, references))

      fields
        .entry(RequestModel.Headers)
        .map(f => result += RamlParametersEmitter("headers", f, ordering, references))
      fields
        .entry(RequestModel.Payloads)
        .map(f => result += spec.factory.payloadsEmitter("body", f, ordering, references))

      fields
        .entry(RequestModel.BaseUriParameters)
        .map(f => result += RamlParametersEmitter(baseUriParameterKey, f, ordering, references))
    }

    fs.entry(OperationModel.Responses)
      .map(f => result += RamlResponsesEmitter("responses", f, ordering, references))

    fs.entry(OperationModel.Security)
      .map(f => result += ParametrizedSecuritiesSchemeEmitter("securedBy", f, ordering))

    result
  }

  override def emit(b: EntryBuilder): Unit = {
    val fs = operation.fields
    sourceOr(
      operation.annotations,
      b.complexEntry(
        ScalarEmitter(fs.entry(OperationModel.Method).get.scalar).emit(_),
        _.obj { b =>
          traverse(ordering.sorted(entries(fs)), b)
        }
      )
    )
  }

  override def position(): Position = pos(operation.annotations)
}