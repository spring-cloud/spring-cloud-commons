This module supports swapping different JSON libraries behind a common startegy thus abstracting developers from dealing with details of individual JSON libraries.

*What it provides:*

*JasonMapper* strategy which contains two operations `byte[] toJson(Object)` and `T fromJson(Object, Type)`. *JasonMapper* has 3 implementations `JacksonMapper` (Jackson3), `Jackson2Mapper`(Jackson2) and `GsonMapper` (Gson). Default is `JacksonMapper` However it depends on existance of Jackson3 libraries in the classpath. This also means that if Jackson3 is not present but Jackson2 is then it will default to that and so on (same for Gson). If multiple libraries are present one can use `spring.cloud.preferred-json-mapper` property to provide values such as `jackson`, `jackson2` or `gson`. One suggestion was to instead provide fully qualified class name of the underlying mapper instead (e.g.,`spring.cloud.preferred-json-mapper=tools.jackson.databind.ObjectMapper`).


*What is doesn't have yet:*

Ability to configure mappers with features and oter configurations. We can add this by exposing a configuration strategy.