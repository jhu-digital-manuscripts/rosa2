package rosa.iiif.search.endpoint;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import rosa.archive.core.ArchiveCoreModule;

public class IIIFSearchConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new IIIFSearchModule(), new ArchiveCoreModule());
    }
}
