# Choral Maven Plugin

Provides the `epp` goal, which is the Maven equivalent of `choral epp`. It projects one or more choreographies and adds the generated Java files to the project's compile sources.

## Build the plugin

Until the plugin is published, install it in your local Maven repository:

```shell
mvn install
```

## Use the `epp` goal

Put Choral sources in `src/main/choral`. To compile one Choral class named `HelloRoles`, use this minimum configuration:

```xml
<properties>
  <!-- Configure which version of the Choral compiler you want here. -->
  <choral.version>0.1.12</choral.version>
  <!-- Configure which version of Choral Maven Plugin you want here. -->
  <choral-maven-plugin.version>1.0.0-SNAPSHOT</choral-maven-plugin.version>
  <maven.compiler.release>8</maven.compiler.release>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.choral-lang</groupId>
      <artifactId>choral-maven-plugin</artifactId>
      <version>${choral-maven-plugin.version}</version>

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
            <!-- Compile the following Choral classes: -->
            <symbols>
              <symbol>HelloRoles</symbol>
            </symbols>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The goal runs in `generate-sources` and writes to `target/generated-sources/choral` by default.
