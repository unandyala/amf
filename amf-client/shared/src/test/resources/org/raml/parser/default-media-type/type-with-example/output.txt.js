Model: file://amf-client/shared/src/test/resources/org/raml/parser/default-media-type/type-with-example/input.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/default-media-type/type-with-example/input.raml#/declarations/types/Person_validation_firstName_validation_minCount/prop
  Message: Data at //firstName must have min. cardinality 1
Data at //lastName must have min. cardinality 1
Data at / must be an object

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/default-media-type/type-with-example/input.raml#/web-api/end-points/%2Fok0/post/request/default/example/default-example
  Property: file://amf-client/shared/src/test/resources/org/raml/parser/default-media-type/type-with-example/input.raml#/web-api/end-points/%2Fok0/post/request/default/example/default-example
  Position: Some(LexicalInformation([(28,15)-(28,20)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/default-media-type/type-with-example/input.raml