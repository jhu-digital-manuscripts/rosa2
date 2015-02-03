package rosa.archive.tool.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Arrays;

/**
 *
 */
public class ToolConfig {

    @Inject @Named("archive.path")
    private String archivePath;

    @Inject @Named("collection.ignore")
    private String collectionIgnore;

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public boolean ignore(String collection) {
        return Arrays.asList(collectionIgnore.split(",")).contains(collection);
    }
}
