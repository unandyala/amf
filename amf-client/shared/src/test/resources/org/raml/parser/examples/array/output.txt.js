Model: file://amf-client/shared/src/test/resources/org/raml/parser/examples/array/input.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/examples/array/input.raml#/declarations/types/array/Emails/array/default-array_validation/prop
  Message: Array items at / must be valid
Object at //items must be valid
Scalar at //items/active must have data type http://www.w3.org/2001/XMLSchema#boolean

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/examples/array/input.raml#/declarations/types/array/Emails/example/default-example
  Property: http://www.w3.org/1999/02/22-rdf-syntax-ns#member
  Position: Some(LexicalInformation([(11,0)-(14,25)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/examples/array/input.raml