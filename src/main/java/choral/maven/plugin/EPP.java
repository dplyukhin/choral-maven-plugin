package choral.maven.plugin;

import choral.Choral;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
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

  /** Directories in which to find Choral header files. */
  @Parameter private List<File> headers;

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
    List<String> args = new ArrayList<>();
    args.add("epp");
    args.add("--sources=" + sourceDirectory.getAbsolutePath());
    args.add("--target=" + outputDirectory.getAbsolutePath());

    if (headers != null) {
      String s =
          headers.stream()
              .map(File::getAbsolutePath)
              .collect(Collectors.joining(File.pathSeparator));
      args.add("--headers=" + s);
    }

    List<File> classpath = requestedClasspath();
    if (!classpath.isEmpty()) {
      args.add(
          "--classpath="
              + classpath.stream()
                  .map(File::getAbsolutePath)
                  .collect(Collectors.joining(File.pathSeparator)));
    }

    if (annotate) {
      args.add("--annotate");
    }
    if (inferComms) {
      args.add("--infer-comms");
    }

    List<String> requestedWorlds = new ArrayList<>();
    if (worlds != null) {
      worlds.stream()
          .map(String::trim)
          .filter(world -> !world.isEmpty())
          .forEach(requestedWorlds::add);
    }

    List<String> requestedSymbols = requestedSymbols();
    for (String requestedSymbol : requestedSymbols) {
      List<String> arguments = new ArrayList<>(args);
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

  private List<String> requestedSymbols() throws MojoFailureException {
    if (symbol == null && symbols == null) {
      throw new MojoFailureException("either symbol or symbols must be set");
    }
    if (symbol != null && symbols != null) {
      throw new MojoFailureException("symbol and symbols are mutually exclusive");
    }
    if (symbol != null) {
      return List.of(symbol);
    }
    return symbols;
  }

  private List<File> requestedClasspath() throws MojoExecutionException, MojoFailureException {
    String version = readChoralVersion();
    boolean unsupported = new ComparableVersion(version).compareTo(MINIMUM_CLASSPATH_VERSION) < 0;
    if (unsupported) {
      if (classpathEntries != null)
        throw new MojoFailureException(
            "Classpath parameter requires Choral 0.1.13+; your version is " + version);
      else return Collections.emptyList();
    }

    List<File> classpath = new ArrayList<>();
    if (classpathEntries != null) {
      classpath.addAll(classpathEntries);
    }
    if (compileClasspathElements != null) {
      compileClasspathElements.stream().map(File::new).forEach(classpath::add);
    }
    return classpath;
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
}
