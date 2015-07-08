package rosa.website.core.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import rosa.archive.core.ArchiveCoreModule;

public class RosaWebsiteGuiceConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new RosaWebsiteModule(), new ArchiveCoreModule());
    }
}
