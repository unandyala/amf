Model: file://amf-client/shared/src/test/resources/validations/discriminator/invalid/closed-parent.raml
Profile: RAML 1.0
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: should NOT have additional properties
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/discriminator/invalid/closed-parent.raml#/web-api/end-points/%2Fendpoint1/get/request/application%2Fjson/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/discriminator/invalid/closed-parent.raml#/web-api/end-points/%2Fendpoint1/get/request/application%2Fjson/schema/example/default-example
  Position: Some(LexicalInformation([(19,0)-(21,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/discriminator/invalid/closed-parent.raml
