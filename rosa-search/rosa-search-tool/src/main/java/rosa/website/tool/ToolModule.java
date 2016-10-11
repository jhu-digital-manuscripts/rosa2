package rosa.website.tool;

import com.google.inject.AbstractModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ToolModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    private Properties loadProps(String path) {
        Properties props = new Properties();

        try (InputStream in = getClass().getResourceAsStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }

        return props;
    }

    private Properties loadSystemProps() {
        return System.getProperties();
    }

}
