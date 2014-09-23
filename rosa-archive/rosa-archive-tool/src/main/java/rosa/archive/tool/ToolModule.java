package rosa.archive.tool;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import rosa.archive.tool.config.ToolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Dependency injection using Google Guice.
 */
public class ToolModule extends AbstractModule {

    @Override
    protected void configure() {

        // Properties
        Names.bindProperties(binder(), getProperties());
        bind(ToolConfig.class);

    }

    private Properties getProperties() {
        Properties props = new Properties();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("tool-config.properties")) {
            props.load(in);
        } catch (IOException e) {
            // TODO log
        }

        return props;
    }

}
