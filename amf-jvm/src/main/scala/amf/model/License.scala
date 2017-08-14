package amf.model

import amf.model.builder.LicenseBuilder

/**
  * License jvm class
  */
case class License private[model] (private val license: amf.domain.License) extends DomainElement {

  val url: String = license.url

  val name: String = license.name

  def toBuilder: LicenseBuilder = LicenseBuilder(license.toBuilder)

  override def equals(other: Any): Boolean = other match {
    case that: License =>
      (that canEqual this) &&
        license == that.license
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[License]

  override private[amf] def element: amf.domain.License = license

}
