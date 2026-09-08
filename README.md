# Choral Maven Plugin

The Choral Maven Plugin runs endpoint projection as part of a Maven build. Its `epp` goal is the
Maven equivalent of `choral epp`: it projects one or more choreographies and adds the generated
Java files to the project's compile sources.

The plugin requires Maven 3.9.9 or newer and a JDK 17 or newer to run the Choral compiler. Choral's
generated Java can still be compiled for Java 8 when its runtime dependencies are also compatible
with Java 8.

## Build the plugin

Until the plugin is published, install it in your local Maven repository:

```shell
mvn install
```

## Use the `epp` goal

Put Choral sources in `src/main/choral`. For a choreography named `HelloRoles`, use this minimum
configuration:

```xml
<properties>
  <choral.version>0.1.12</choral.version>
  <choral-maven-plugin.version>1.0.0-SNAPSHOT</choral-maven-plugin.version>
  <maven.compiler.release>8</maven.compiler.release>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.choral-lang</groupId>
      <artifactId>choral-maven-plugin</artifactId>
      <version>${choral-maven-plugin.version}</version>

      <!-- Override the compiler version bundled by the plugin. -->
      <dependencies>
        <dependency>
          <groupId>org.choral-lang</groupId>
          <artifactId>choral</artifactId>
          <version>${choral.version}</version>
        </dependency>
      </dependencies>

      <executions>
        <execution>
          <goals>
            <goal>epp</goal>
          </goals>
          <configuration>
            <symbol>HelloRoles</symbol>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The goal runs in `generate-sources` and writes to
`target/generated-sources/choral` by default. The generated directory is registered with Maven, so
the normal Java compiler compiles its contents later in the same build.

## Configuration

| Parameter | Default | Description |
| --- | --- | --- |
| `symbol` | empty | One choreography to project. |
| `symbols` | empty | Choreographies to project sequentially. |
| `sourceDirectory` | `src/main/choral` | Directory containing `.ch` files. |
| `outputDirectory` | `target/generated-sources/choral` | Directory for generated Java. |
| `headerDirectories` | empty | Additional directories containing `.chh` files. |
| `worlds` | empty | Roles to project; empty projects every role. |
| `classpathEntries` | empty | Java class directories or JARs to add to the resolved Maven compile classpath. Requires Choral 0.1.13+. |
| `annotate` | `false` | Add Choral's `@Choreography` annotation to generated artifacts. |
| `inferComms` | `false` | Infer missing communications and selections. |
| `skip` | `false` | Skip endpoint projection. |

Lists are configured with nested elements:

```xml
<configuration>
  <symbols>
    <symbol>HelloRoles</symbol>
    <symbol>GoodbyeRoles</symbol>
  </symbols>
  <worlds>
    <world>A</world>
    <world>B</world>
  </worlds>
  <headerDirectories>
    <headerDirectory>${project.basedir}/src/main/choral-headers</headerDirectory>
  </headerDirectories>
</configuration>
```

At least one nonblank value must be supplied through `symbol` or `symbols`. If both are present,
the singular value is projected first. Duplicate names are projected only once.

### Choral classpath

The plugin always asks Maven to resolve the consuming project's compile dependencies. With Choral
0.1.13 or newer, their classpath entries are passed to the compiler automatically. Explicit
`classpathEntries` are normalized, de-duplicated, and appended to that Maven classpath.

Classpath support was added to Choral after 0.1.12. With an older compiler, the resolved Maven
classpath is ignored so ordinary projection continues to work. Configuring explicit
`classpathEntries` with an older compiler fails with a clear version error.

```xml
<properties>
  <choral.version>0.1.13</choral.version>
</properties>

<configuration>
  <symbol>HelloRoles</symbol>
  <classpathEntries>
    <classpathEntry>${project.basedir}/lib/example.jar</classpathEntry>
  </classpathEntries>
</configuration>
```

## Test

Run the plugin's build and consumer-project integration tests with:

```shell
mvn verify
```
