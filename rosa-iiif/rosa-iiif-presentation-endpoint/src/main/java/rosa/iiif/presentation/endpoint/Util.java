package rosa.iiif.presentation.endpoint;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Util {
    public static final String SERVLET_CONFIG_PATH = "/iiif-servlet.properties";
    private static final String LUCENE_DIRECTORY = "lucene";
    private static final String ARCHIVE_DIRECTORY = "archive";
    
    // Derive the web app path from location of iiif-servlet.properties    
    private static Path get_webapp_path() {
        try {
            return Paths.get(Util.class.getResource(SERVLET_CONFIG_PATH).toURI()).getParent().getParent();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed find webapp path", e);
        }
    }
    
    public static Path getArchivePath() {
        return get_webapp_path().resolve(ARCHIVE_DIRECTORY);
    }
    
    public static Path getLucenePath() {
        return get_webapp_path().resolve(LUCENE_DIRECTORY);
    }
}
