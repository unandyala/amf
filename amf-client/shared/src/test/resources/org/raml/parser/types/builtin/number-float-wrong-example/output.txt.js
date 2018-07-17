Model: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml
Profile: RAML
Conforms? false
Number of results: 3

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/MyCustomType_validation_validation_multipleOf/prop
  Message: Data at is not a multipleOf '3'
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/MyCustomType/example/default-example
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(10,15)-(10,20)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/OtherCustomType_validation_validation_minimum/prop
  Message: Data at / must be greater than or equal to 2.5
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/OtherCustomType/example/bad1
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(18,14)-(18,17)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/OtherCustomType_validation_validation_maximum/prop
  Message: Data at / must be smaller than or equal to 5.3
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml#/declarations/types/scalar/OtherCustomType/example/bad2
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(19,14)-(19,17)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/types/builtin/number-float-wrong-example/input.raml