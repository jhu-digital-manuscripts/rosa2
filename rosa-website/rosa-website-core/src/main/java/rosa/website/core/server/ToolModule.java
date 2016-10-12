package rosa.website.core.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.search.core.LuceneMapper;
import rosa.website.search.WebsiteLuceneMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ToolModule extends AbstractModule {
    private static final String TOOL_PROPERTIES = "tool.properties";

    @Override
    protected void configure() {
        bind(Store.class);
        bind(LuceneMapper.class).to(WebsiteLuceneMapper.class);
    }

    private Properties loadProperties(String path) {
        Properties props = new Properties();

        try (InputStream in = getClass().getResourceAsStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + path, e);
        }

        return props;
    }

    @Provides
    public Store store(SerializerSet serializerSet, BookChecker bookChecker, BookCollectionChecker collectionChecker,
                       ByteStreamGroup archive) {
        return new StoreImpl(serializerSet, bookChecker, collectionChecker, archive);
    }

    @Provides
    public ByteStreamGroup archiveByteStreams(@Named("archive.path") String archivePath) {
        return new FSByteStreamGroup(archivePath);
    }

    @Provides
    @Named("archive.path")
    public String archivePath() {
        String path = System.getProperty("archive.path");
        if (path != null && !path.isEmpty()) {
            return path;
        }

        return loadProperties(TOOL_PROPERTIES).getProperty("archive.path");
    }
}
