package choral.maven.plugin;

import choral.Choral;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Compiles Choral source code into Java. */
@Mojo(
    name = "epp",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = false)
public class EPP extends AbstractMojo {
  private static final ComparableVersion MINIMUM_CLASSPATH_VERSION =
      new ComparableVersion("0.1.13");

  /** The fully qualified name of a choreography to compile. */
  @Parameter(property = "choral.epp.symbol")
  private String symbol;

  /** The fully qualified names of choreographies to compile. */
  @Parameter private List<String> symbols;

  /** Where to find Choral source files. */
  @Parameter(
      property = "choral.epp.sourceDirectory",
      defaultValue = "${project.basedir}/src/main/choral",
      required = true)
  private File sourceDirectory;

  /** Where to find Choral header files. */
  @Parameter private List<File> headerDirectories;

  /** Where to find Java JARs and classfiles. */
  @Parameter private List<File> classpathEntries;

  /** Where to put generated Java files. */
  @Parameter(
      property = "choral.epp.outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources/choral",
      required = true)
  private File outputDirectory;

  /** List of roles to project. An empty list projects every role. */
  @Parameter private List<String> worlds;

  /** Annotate generated Java classes with {@code @Choreography}. */
  @Parameter(property = "choral.epp.annotate", defaultValue = "true")
  private boolean annotate;

  /** Infer missing communications and selections. */
  @Parameter(property = "choral.epp.inferComms", defaultValue = "false")
  private boolean inferComms;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
  private List<String> compileClasspathElements;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<String> requestedSymbols = requestedSymbols();
    validateConfiguration(requestedSymbols);

    List<String> commonArguments = new ArrayList<>();
    commonArguments.add("epp");
    commonArguments.add("--sources=" + sourceDirectory.getAbsolutePath());
    commonArguments.add("--target=" + outputDirectory.getAbsolutePath());

    List<File> headers = headerDirectories == null ? Collections.emptyList() : headerDirectories;
    if (!headers.isEmpty()) {
      commonArguments.add("--headers=" + joinFiles(headers));
    }

    List<File> explicitClasspath =
        classpathEntries == null ? Collections.emptyList() : classpathEntries;
    addClasspathArgument(commonArguments, explicitClasspath);

    if (annotate) {
      commonArguments.add("--annotate");
    }
    if (inferComms) {
      commonArguments.add("--infer-comms");
    }

    List<String> requestedWorlds = new ArrayList<>();
    if (worlds != null) {
      worlds.stream()
          .map(String::trim)
          .filter(world -> !world.isEmpty())
          .forEach(requestedWorlds::add);
    }

    for (String requestedSymbol : requestedSymbols) {
      List<String> arguments = new ArrayList<>(commonArguments);
      arguments.add(requestedSymbol);
      arguments.addAll(requestedWorlds);

      getLog().info("Projecting Choral choreography " + requestedSymbol);
      final int exitCode;
      try {
        exitCode = Choral.compile(arguments.toArray(new String[0]));
      } catch (RuntimeException e) {
        throw new MojoExecutionException(
            "Unable to project Choral choreography " + requestedSymbol, e);
      }

      if (exitCode != 0) {
        throw new MojoFailureException(
            "Choral endpoint projection failed for "
                + requestedSymbol
                + " with exit code "
                + exitCode);
      }
    }

    project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
  }

  private void validateConfiguration(List<String> requestedSymbols) throws MojoFailureException {
    if (requestedSymbols.isEmpty()) {
      throw new MojoFailureException(
          "At least one Choral choreography must be configured with symbol or symbols");
    }
    if (!sourceDirectory.isDirectory()) {
      throw new MojoFailureException("Choral source directory does not exist: " + sourceDirectory);
    }
    if (headerDirectories != null) {
      for (File headerDirectory : headerDirectories) {
        if (headerDirectory == null || !headerDirectory.isDirectory()) {
          throw new MojoFailureException(
              "Choral header directory does not exist: " + headerDirectory);
        }
      }
    }
    if (classpathEntries != null) {
      for (File classpathEntry : classpathEntries) {
        if (classpathEntry == null || !classpathEntry.exists()) {
          throw new MojoFailureException(
              "Choral classpath entry does not exist: " + classpathEntry);
        }
      }
    }
  }

  private List<String> requestedSymbols() {
    Set<String> requested = new LinkedHashSet<>();
    if (symbol != null && !symbol.trim().isEmpty()) {
      requested.add(symbol.trim());
    }
    if (symbols != null) {
      symbols.stream()
          .filter(value -> value != null)
          .map(String::trim)
          .filter(value -> !value.isEmpty())
          .forEach(requested::add);
    }
    return new ArrayList<>(requested);
  }

  private void addClasspathArgument(List<String> arguments, List<File> explicitClasspath)
      throws MojoExecutionException, MojoFailureException {
    String version = readChoralVersion();
    if (new ComparableVersion(version).compareTo(MINIMUM_CLASSPATH_VERSION) < 0) {
      if (!explicitClasspath.isEmpty()) {
        throw new MojoFailureException(
            "Choral classpath support requires Choral 0.1.13 or newer; loaded Choral " + version);
      }
      return;
    }

    Set<String> classpath = new LinkedHashSet<>();
    if (compileClasspathElements != null) {
      compileClasspathElements.stream()
          .filter(value -> value != null && !value.trim().isEmpty())
          .map(EPP::normalizePath)
          .forEach(classpath::add);
    }
    explicitClasspath.stream()
        .map(File::getAbsolutePath)
        .map(EPP::normalizePath)
        .forEach(classpath::add);

    if (!classpath.isEmpty()) {
      arguments.add("--classpath=" + String.join(File.pathSeparator, classpath));
    }
  }

  private String readChoralVersion() throws MojoExecutionException {
    String resource = "/META-INF/maven/org.choral-lang/choral/pom.properties";
    try (InputStream input = Choral.class.getResourceAsStream(resource)) {
      if (input == null) {
        throw new MojoExecutionException("Unable to determine the loaded Choral compiler version");
      }
      Properties properties = new Properties();
      properties.load(input);
      String version = properties.getProperty("version");
      if (version == null || version.trim().isEmpty()) {
        throw new MojoExecutionException("Loaded Choral compiler does not declare its version");
      }
      return version.trim();
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to read the loaded Choral compiler version", e);
    }
  }

  private static String joinFiles(List<File> files) {
    return files.stream()
        .map(File::getAbsolutePath)
        .collect(Collectors.joining(File.pathSeparator));
  }

  private static String normalizePath(String value) {
    return new File(value).toPath().toAbsolutePath().normalize().toString();
  }
}
