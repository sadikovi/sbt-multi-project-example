name := "hello"

version := "0.1.1"

mainClass in assembly := Some("org.hello.foo.Foo")

assemblyJarName in assembly := "helloproject.jar"

test in assembly := {}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)
