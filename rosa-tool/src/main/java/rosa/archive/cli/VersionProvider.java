package rosa.archive.cli;

import picocli.CommandLine.IVersionProvider;

/**
 * Supplies the {@code --version} text from the jar manifest's {@code Implementation-Version},
 * which the Maven build sets from the project version, so released jars report their release.
 */
public final class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        Package pkg = Rosa2Cli.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        return new String[] { "rosa-tool " + (version != null ? version : "(development build)") };
    }
}
