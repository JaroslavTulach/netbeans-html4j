# Type-safe Parsing of JSON without any Reflection

This example demonstrates the power of ahead-of-time compilation of Java
into a native binary (assembled by [GraalVM](http://www.graalvm.org/) RC6)
and shows how to wire everything together to parse JSON documents.

[GraalVM](http://www.graalvm.org/) provides tool called `native-image` that
compiles Java bytecode to native executable. However, the tool isn't fully
featured implementation of Java. It has several limitations. One of them
is limited support for reflection. Finding a type-safe JSON parser in Java
that doesn't need reflection isn't easy. Luckily there is one: the
[Apache @net.java.html.json.Model annotation](https://github.com/apache/incubator-netbeans-html4j)
doesn't require any reflection to work properly.

## Setting the project up

To try this example, check it out first and go into the appropriate directory:
```bash
$ git clone https://github.com/jaroslavtulach/incubator-netbeans-html4j/ -b examples
$ cd incubator-netbeans-html4j/examples/jsonparse/
```
Now you are ready to run the code.
However, rather than using regular JDK8 (that doesn't support `native-image` command),
it is recommended to download and set [GraalVM](http://www.graalvm.org/) up:
```bash
$ mvn -q package exec:java@test
```
If you execute the above command on a regular JDK a browser with instructions
to download [GraalVM](http://www.graalvm.org/) is opened. After
downloading you can re-run the command as:
```bash
$ JAVA_HOME=/path/graalvm mvn -q package exec:java@test
there is 14 repositories
repository examples is owned by graalvm
repository graal-js-archetype is owned by graalvm
repository graal-jvmci-8 is owned by graalvm
repository graaljs is owned by graalvm
repository graalpython is owned by graalvm
repository graalvm-demos is owned by graalvm
repository graalvm.github.io is owned by graalvm
repository mx is owned by graalvm
repository mysql-mle-demos is owned by graalvm
repository openjdk8-jvmci-builder is owned by graalvm
repository retext is owned by graalvm
repository scaladays2018-demos is owned by graalvm
repository simplelanguage is owned by graalvm
repository sulong is owned by graalvm

```
The sample program connects to GitHub API and prints information obtained from
the received document via the `@Model` based parser.

## How does this work?

The parsing code is in a single class [ParseJSON](https://github.com/JaroslavTulach/incubator-netbeans-html4j/blob/examples/examples/jsonparse/src/main/java/org/apidesign/demo/jsonparse/ParseJSON.java).
First of all it defines (potentially simplified) structure of the JSON document
```java
@Model(className="RepositoryInfo", properties = {
    @Property(name = "id", type = int.class),
    @Property(name = "name", type = String.class),
    @Property(name = "owner", type = Owner.class),
    @Property(name = "private", type = boolean.class),
})
final class RepositoryCntrl {
    @Model(className = "Owner", properties = {
        @Property(name = "login", type = String.class)
    })
    static final class OwnerCntrl {
    }
}
```
and then it uses the generated `RepositoryInfo` and `Owner` classes to parse
the provided input stream and pick the important information up while doing that:
```java
List<RepositoryInfo> repositories = new ArrayList<>();
try (InputStream is = initializeStream(args)) {
    Models.parse(CONTEXT, RepositoryInfo.class, is, repositories);
}

System.err.println("there is " + repositories.size() + " repositories");
repositories.stream().filter((repo) -> repo != null && repo.getOwner() != null).forEach((repo) -> {
    System.err.println("repository " + repo.getName() + " is owned by " + repo.getOwner().getLogin());
});
```
Few lines of code and all the information is ready in your hands in a type-safe
way.

## Going Native!

Now it is the time to go native and compile your sample Java application into
native binary. Just execute:
```bash
$ JAVA_HOME=/pathto/graalvm mvn -q package exec:exec@generate-binary
Build on Server(pid: 1234, port: 42437)
[jsonparse:1234]    classlist:   1,277.11 ms
[jsonparse:1234]        (cap):   1,638.03 ms
[jsonparse:1234]        setup:   4,385.15 ms
[jsonparse:1234]   (typeflow):  17,787.02 ms
[jsonparse:1234]    (objects):  10,596.14 ms
[jsonparse:1234]   (features):     464.39 ms
[jsonparse:1234]     analysis:  29,278.01 ms
[jsonparse:1234]     universe:   1,192.72 ms
[jsonparse:1234]      (parse):   4,669.14 ms
[jsonparse:1234]     (inline):   4,771.81 ms
[jsonparse:1234]    (compile):  48,711.44 ms
[jsonparse:1234]      compile:  59,436.80 ms
[jsonparse:1234]        image:   3,152.13 ms
[jsonparse:1234]        write:     502.66 ms
[jsonparse:1234]      [total]:  99,387.28 ms
```
In less than two minutes a fully optimized native binary representing your Java
application has been prepared for you. It contains a JSON parser and can be
tested via Maven or invoked directly:
```bash
$ mvn -q exec:exec@test-binary
there is 14 repositories
repository examples is owned by graalvm
repository graal-js-archetype is owned by graalvm
repository graal-jvmci-8 is owned by graalvm
repository graaljs is owned by graalvm
repository graalpython is owned by graalvm
repository graalvm-demos is owned by graalvm
repository graalvm.github.io is owned by graalvm
repository mx is owned by graalvm
repository mysql-mle-demos is owned by graalvm
repository openjdk8-jvmci-builder is owned by graalvm
repository retext is owned by graalvm
repository scaladays2018-demos is owned by graalvm
repository simplelanguage is owned by graalvm
repository sulong is owned by graalvm
#
# or invoked directly:
#
$ curl https://api.github.com/users/graalvm/repos | ./target/jsonparse -
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 43041  100 43041    0     0  31925      0  0:00:01  0:00:01 --:--:-- 31929
there is 14 repositories
repository examples is owned by graalvm
repository graal-js-archetype is owned by graalvm
repository graal-jvmci-8 is owned by graalvm
repository graaljs is owned by graalvm
repository graalpython is owned by graalvm
repository graalvm-demos is owned by graalvm
repository graalvm.github.io is owned by graalvm
repository mx is owned by graalvm
repository mysql-mle-demos is owned by graalvm
repository openjdk8-jvmci-builder is owned by graalvm
repository retext is owned by graalvm
repository scaladays2018-demos is owned by graalvm
repository simplelanguage is owned by graalvm
repository sulong is owned by graalvm
```

Your Java code has just gone native. Together with a reflection less JSON
parser ready to parse your documents in a type-safe way.
