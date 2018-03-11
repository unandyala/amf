package amf.plugins.document.vocabularies2

import amf.ProfileNames
import amf.core.Root
import amf.core.client.GenerationOptions
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.core.plugins.{AMFDocumentPlugin, AMFPlugin, AMFValidationPlugin}
import amf.core.registries.AMFDomainEntityResolver
import amf.core.remote.Platform
import amf.core.services.RuntimeValidator
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations, SeverityLevels, ValidationResultProcessor}
import amf.plugins.document.vocabularies2.annotations.AliasesLocation
import amf.plugins.document.vocabularies2.emitters.dialects.{RamlDialectEmitter, RamlDialectLibraryEmitter}
import amf.plugins.document.vocabularies2.emitters.instances.RamlDialectInstancesEmitter
import amf.plugins.document.vocabularies2.emitters.vocabularies.RamlVocabularyEmitter
import amf.plugins.document.vocabularies2.metamodel.document._
import amf.plugins.document.vocabularies2.metamodel.domain._
import amf.plugins.document.vocabularies2.model.document.{Dialect, DialectInstance, DialectLibrary, Vocabulary}
import amf.plugins.document.vocabularies2.parser.ExtensionHeader
import amf.plugins.document.vocabularies2.parser.common.RAMLExtensionsReferenceHandler
import amf.plugins.document.vocabularies2.parser.dialects.{DialectContext, RamlDialectsParser}
import amf.plugins.document.vocabularies2.parser.instances.{DialectInstanceContext, RamlDialectInstanceParser}
import amf.plugins.document.vocabularies2.parser.vocabularies.{RamlVocabulariesParser, VocabularyContext}
import amf.plugins.document.vocabularies2.resolution.pipelines.{DialectInstanceResolutionPipeline, DialectResolutionPipeline}
import amf.plugins.document.vocabularies2.validation.AMFDialectValidations
import org.yaml.model.{YComment, YDocument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RamlHeaderExtractor {
  def comment(root: Root): Option[YComment] = root.parsed.comment

  def comment(document: YDocument): Option[YComment] =
    document.children.find(v => v.isInstanceOf[YComment]).asInstanceOf[Option[YComment]]
}

object DialectHeader extends RamlHeaderExtractor {
  def apply(root: Root): Boolean = comment(root) match {
    case Some(comment: YComment) =>
      comment.metaText match {
        case t if t.startsWith("%RAML 1.0 Vocabulary") => true
        case t if t.startsWith("%RAML 1.0 Dialect")    => true
        case t if t.startsWith("%RAML 1.0")            => false
        case t if t.startsWith("%RAML 0.8")            => false
        case t if t.startsWith("%")                    => true
        case _                                         => false
      }
    case _ => false
  }
}

object RAMLVocabulariesPlugin extends AMFDocumentPlugin with RamlHeaderExtractor with AMFValidationPlugin with ValidationResultProcessor {

  val registry = new DialectsRegistry

  override val ID: String = "RAML Vocabularies2"

  override val vendors: Seq[String] = Seq("RAML Vocabularies2")

  override def init(): Future[AMFPlugin] = Future { this }

  override def modelEntities: Seq[Obj] = Seq(
    VocabularyModel,
    ExternalModel,
    VocabularyReferenceModel,
    ClassTermModel,
    ObjectPropertyTermModel,
    DatatypePropertyTermModel,
    DialectModel,
    NodeMappingModel,
    PropertyMappingModel,
    DocumentsModelModel,
    PublicNodeMappingModel,
    DocumentMappingModel,
    DialectLibraryModel,
    DialectFragmentModel,
    DialectInstanceModel,
    DialectInstanceLibraryModel,
    DialectInstanceFragmentModel
  )

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = Map(
    "aliases-location" -> AliasesLocation
  )

  /**
    * Resolves the provided base unit model, according to the semantics of the domain of the document
    */
  override def resolve(unit: BaseUnit): BaseUnit = unit match {
    case dialect: Dialect         => new DialectResolutionPipeline().resolve(dialect)
    case dialect: DialectInstance => new DialectInstanceResolutionPipeline().resolve(dialect)
    case _                        => unit
  }

  /**
    * List of media types used to encode serialisations of
    * this domain
    */
  override def documentSyntaxes: Seq[String] = Seq(
    "application/raml",
    "application/raml+json",
    "application/raml+yaml",
    "text/yaml",
    "text/x-yaml",
    "application/yaml",
    "application/x-yaml"
  )

  /**
    * Parses an accepted document returning an optional BaseUnit
    */
  override def parse(document: Root, parentContext: ParserContext, platform: Platform): Option[BaseUnit] = {
    comment(document) match {
      case Some(comment) =>
        comment.metaText match {
          case ExtensionHeader.VocabularyHeader      => Some(new RamlVocabulariesParser(document)(new VocabularyContext(parentContext)).parseDocument())
          case ExtensionHeader.DialectLibraryHeader  => Some(new RamlDialectsParser(document)(new DialectContext(parentContext)).parseLibrary())
          case ExtensionHeader.DialectFragmentHeader => Some(new RamlDialectsParser(document)(new DialectContext(parentContext)).parseFragment())
          case ExtensionHeader.DialectHeader         => parseAndRegisterDialect(document, parentContext)
          case header                                => parseDialectInstance(header, document, parentContext)
        }
      case _ => None
    }
  }

  /**
    * Unparses a model base unit and return a document AST
    */
  override def unparse(unit: BaseUnit, options: GenerationOptions): Option[YDocument] = unit match {
    case vocabulary: Vocabulary    => Some(RamlVocabularyEmitter(vocabulary).emitVocabulary())
    case dialect: Dialect          => Some(RamlDialectEmitter(dialect).emitDialect())
    case library: DialectLibrary   => Some(RamlDialectLibraryEmitter(library).emitDialectLibrary())
    case instance: DialectInstance => Some(RamlDialectInstancesEmitter(instance, registry.dialectFor(instance).get).emitInstance())
    case _                         => None
  }

  /**
    * Decides if this plugin can parse the provided document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will parse the document base on information from
    * the document structure
    */
  override def canParse(document: Root): Boolean = DialectHeader(document)

  /**
    * Decides if this plugin can unparse the provided model document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will unparse the document base on information from
    * the instance type and properties
    */
  override def canUnparse(unit: BaseUnit): Boolean = unit match {
    case _: Vocabulary             => true
    case _: Dialect                => true
    case _: DialectLibrary         => true
    case instance: DialectInstance => registry.knowsDialectInstance(instance)
    case _                         => false
  }

  override def referenceHandler(): ReferenceHandler = new RAMLExtensionsReferenceHandler()

  override def dependencies(): Seq[AMFPlugin] = Seq()

  override def modelEntitiesResolver: Option[AMFDomainEntityResolver] = Some(registry)

  protected def parseAndRegisterDialect(document: Root, parentContext: ParserContext) = {
    new RamlDialectsParser(document)(new DialectContext(parentContext)).parseDocument() match {
      case dialect: Dialect =>
        registry.register(dialect)
        Some(dialect)
      case unit => Some(unit)
    }
  }

  protected def parseDialectInstance(header: String, document: Root, parentContext: ParserContext): Option[BaseUnit] = {
    registry.withRegisteredDialect(header) { dialect =>
      if (header.split("\\|").head.replace(" ", "") == dialect.header)
        new RamlDialectInstanceParser(document)(new DialectInstanceContext(dialect, parentContext)).parseDocument()
      else if (dialect.isFragmentHeader(header))
        new RamlDialectInstanceParser(document)(new DialectInstanceContext(dialect, parentContext)).parseFragment()
      else if (dialect.isLibraryHeader(header))
        new RamlDialectInstanceParser(document)(new DialectInstanceContext(dialect, parentContext)).parseLibrary()
      else
        throw new Exception("Dialect instances libraries not supported yet")
    }
  }

  // Cache for the validation profiles
  var validationsProfilesMap: Map[String, ValidationProfile] = Map()

  /**
    * Validation profiles supported by this plugin. Notice this will be called multiple times.
    */
  override def domainValidationProfiles(platform: Platform): Map[String, () => ValidationProfile] = {
    registry.allDialects().foldLeft(Map[String, () => ValidationProfile]()) {
      case (acc, dialect) if !dialect.nameAndVersion().contains("Validation Profile") =>
        acc.updated(dialect.nameAndVersion(), () => {
          val profileName = dialect.nameAndVersion()
          validationsProfilesMap.get(profileName) match {
            case Some(profile) => profile
            case _             =>
              val resolvedDialect = new DialectResolutionPipeline().resolve(dialect)
              val profile = new AMFDialectValidations(resolvedDialect).profile()
              validationsProfilesMap += (profileName -> profile)
              profile
          }
        })
      case (acc, _) =>
        acc
    }
  }

  /**
    * Request for validation of a particular model, profile and list of effective validations for that profile
    */
  override def validationRequest(baseUnit: BaseUnit,
                                 profile: String,
                                 validations: EffectiveValidations,
                                 platform: Platform): Future[AMFValidationReport] = {
    for {
      shaclReport <- RuntimeValidator.shaclValidation(baseUnit, validations)
    } yield {

      // todo aggregating parser-side validations ?
      // var results = aggregatedReport.map(r => processAggregatedResult(r, "RAML", validations))

      // adding model-side validations
      val results = shaclReport.results.flatMap { r => buildValidationResult(baseUnit, r, ProfileNames.RAML, validations) }

      AMFValidationReport(
        conforms = !results.exists(_.level == SeverityLevels.VIOLATION),
        model = baseUnit.id,
        profile = profile,
        results = results
      )
    }
  }


}
