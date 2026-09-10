# Choral Maven Plugin ![Latest Release](https://img.shields.io/github/v/release/choral-lang/choral-maven-plugin
) [![Integration Tests](https://github.com/choral-lang/choral-maven-plugin/actions/workflows/integration-tests.yml/badge.svg)](https://github.com/choral-lang/choral-maven-plugin/actions/workflows/integration-tests.yml)

Provides the `epp` goal, which is the Maven equivalent of `choral epp`. It projects one or more choreographies and adds the generated Java files to the project's compile sources.

## Use the `epp` goal

Put Choral sources in `src/main/choral`. To compile a Choral class named `HelloRoles`, use this configuration:

```xml
<properties>
  <!-- Configure which version of the Choral compiler you want here. -->
  <choral.version>0.1.13</choral.version>
  <!-- Configure which version of Choral Maven Plugin you want here. -->
  <choral-maven-plugin.version>0.1.0</choral-maven-plugin.version>
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

## Local Development

### Build the plugin

Install a development snapshot in your local Maven repository:

```shell
mvn install
```

### Issuing a new release

To release the plugin, [publish a GitHub release](https://github.com/choral-lang/choral-maven-plugin/releases/new) whose tag has the exact format `v#.#.#`, for example `v1.0.0`. Publishing the release triggers the [Maven Central Release workflow](https://github.com/choral-lang/choral-maven-plugin/actions/workflows/maven-publish.yml).
