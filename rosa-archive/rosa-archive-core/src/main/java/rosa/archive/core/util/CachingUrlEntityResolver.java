package rosa.archive.core.util;

import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class CachingUrlEntityResolver implements EntityResolver {
    private static final int CACHE_MAX_SIZE = 10;

    private ConcurrentHashMap<String, String> entityCache;

    public CachingUrlEntityResolver() {
        this.entityCache = new ConcurrentHashMap<>();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource is = new InputSource(systemId);

        if (!entityCache.containsKey(systemId)) {
            String i = IOUtils.toString(new URL(systemId), "UTF-8");

            if (entityCache.size() > CACHE_MAX_SIZE) {
                entityCache.clear();
            }

            entityCache.putIfAbsent(systemId, i);
            is.setByteStream(IOUtils.toInputStream(i, "UTF-8"));
        }

        is.setByteStream(IOUtils.toInputStream(entityCache.get(systemId), "UTF-8"));
        return is;
    }
}
