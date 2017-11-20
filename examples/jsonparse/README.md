# Type-safe Parsing of JSON without any Reflection

This example demonstrates the power of ahead-of-time compilation of Java
into a native binary (done by [GraalVM](http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/)'s
`native-image` command) and shows how to bind this together with
parsing of JSON documents.

SubstrateVM (the VM behind `native-image`) has certain limitations. One of them
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
Now you are ready to run the code:
```bash
$ mvn package exec:java
```
However, rather than regular using JDK8 (that doesn't support `native-image` command), 
it is recommended to download set [GraalVM](http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/)
up. If you execute the above command on a regular JDK a browser with instructions
to download [GraalVM](http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/)
is opened. After downloading you can re-run the command as:
```bash
$ JAVA_HOME=/path/graalvm mvn package exec:java 
```
The sample program connects to GitHub API and prints informations obtained from
the parsing of the result.

## How does this work?

The parsing code is in a single class [ParseJSON](https://github.com/JaroslavTulach/incubator-netbeans-html4j/blob/examples/examples/jsonparse/src/main/java/org/apidesign/demo/jsonparse/ParseJSON.java).
First of all it defines (potentially simplified) structure of the JSON document:
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
the provide input stream and pick certain information up while doing that:
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

## Going **Native**!

Now it is time to go native and compile your sample Java application into
native binary. Just execute:
```bash
$ JAVA_HOME=~/bin/graalvm mvn -Psubstratevm package exec:exec
   classlist:   2,797.40 ms
       (cap):     957.45 ms
       setup:   2,000.45 ms
  (typeflow):   6,062.05 ms
   (objects):   1,533.18 ms
  (features):       1.71 ms
    analysis:   7,808.20 ms
    universe:     381.46 ms
     (parse):   1,576.88 ms
    (inline):   1,464.84 ms
   (compile):  15,992.62 ms
     compile:  19,494.88 ms
       image:     993.15 ms
       write:     403.05 ms
     [total]:  33,981.06 ms
```
In less than a minute a fully optimized native binary representing your Java
application has been prepared for you. It contains a JSON parser and can be
tested by consuming the input and extracting the results. Try:
```bash
$ curl https://api.github.com/users/jersey/repos | ./target/jsonparse -
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 43041  100 43041    0     0  31925      0  0:00:01  0:00:01 --:--:-- 31929
there is 8 repositories
repository hol-sse-websocket is owned by jersey
repository jersey is owned by jersey
repository jersey-1.x is owned by jersey
repository jersey-1.x-old is owned by jersey
repository jersey-integration-patches is owned by jersey
repository jersey-old is owned by jersey
repository jersey-web is owned by jersey
repository jersey.github.io is owned by jersey
```

Your Java code has just gone native. Together with a reflection less JSON
parser.
