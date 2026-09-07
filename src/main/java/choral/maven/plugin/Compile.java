package choral.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Compiles Choral code.
 */
@Mojo(name = "compile")
public class Compile extends AbstractMojo
{
    /**
     * Where to find Choral source files.
     */
    @Parameter(property = "compile.sourcePath", defaultValue = "src/main/choral" )
    private String sourcePath;
    /**
     * Where to find Choral header files.
     */
    @Parameter(property = "compile.headersPath", defaultValue = "src/main/resources/choral/headers" )
    private String headersPath;
    /**
     * Where to put generated Java files.
     */
    @Parameter(property = "compile.outputDirectory", defaultValue = "target/generated-sources" )
    private String outputDirectory;
    /**
     * Which version of the Choral compiler to use.
     */
    @Parameter(property = "compile.compilerVersion", defaultValue = "${choral.version}" )
    private String compilerVersion;

    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().info("Hello Choral, version " + compilerVersion + "!");
    }
}