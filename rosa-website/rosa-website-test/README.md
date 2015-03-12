Set up of new (test) web site:

###Create a new GWT module.
Recommend using the maven archetype:

 `mvn archetype:generate -DarchetypeGroupId=org.codehaus.mojo -DarchetypeArtifactId=gwt-maven-plugin -DarchetypeVersion=2.7.0`

When the new module is created, clean out all of the generated code. The module .gwt.xml can be kept and modified. In
the `webapp` directory, the .html and .css can be modified as needed. The `web.xml` descriptor must be cleaned of the
default service that is initially present. All code can be deleted, although the EntryPoint class is useful to modify.

###Module set up
All of the following must be set up.

####&lt;ModuleName&gt;.gwt.xml
The GWT module file.

Import all modules necessary: User, Inject, Logging, Activity, Place, HTTP, I18N, RosaWebsiteModel, RosaWebsiteCore
(RosaWebsiteSearch, RosaWebsiteViewer), Standard. This will make these modules visible to the GWT compiler.

```

    <inherits name='com.google.gwt.user.User' />

    <inherits name="com.google.gwt.inject.Inject"/>
    <inherits name="com.google.gwt.logging.Logging"/>
    <inherits name="com.google.gwt.resources.Resources" />
    <!-- Activities and Places history mechanism -->
    <inherits name="com.google.gwt.activity.Activity"/>
    <inherits name="com.google.gwt.place.Place"/>
    <!-- HTTP stuff, including making HTTP requests, encoding/decoding URLs -->
    <inherits name="com.google.gwt.http.HTTP"/>

    <!-- Constants + internationalization -->
    <inherits name="com.google.gwt.i18n.I18N"/>

    <inherits name="rosa.website.model.RosaWebsiteModel"/>
    <inherits name="rosa.website.core.RosaWebsiteCore"/>

    <inherits name='com.google.gwt.user.theme.standard.Standard' />
```

Set all necessary web site properties. The following properties will set up remote logging.

```

    <!-- Logging -->
    <set-property name="gwt.logging.logLevel" value="FINE" />
    <set-property name="gwt.logging.enabled" value="TRUE"/>
    <set-property name="gwt.logging.simpleRemoteHandler" value="ENABLED" />
```

Set the entry point class.

```

    <entry-point class='rosa.website.test.client.RosaWebsiteTest' />
```

Declare the packages that will be compiled to Javascript.

```

    <source path='client' />
    <source path='shared' />
```

####web.xml
Declare all needed servlets and the welcome file. Servlet: remoteLogging is needed to log client data
on the server. Servlet: resourceService is one of the _rosa-website-core_ services. This one will
load static HTML pages from the server on demand.

```

    <!-- remote logging -->
    <servlet>
        <servlet-name>remoteLogging</servlet-name>
        <servlet-class>com.google.gwt.logging.server.RemoteLoggingServiceImpl</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>remoteLogging</servlet-name>
        <url-pattern>/RosaWebsiteTest/remote_logging</url-pattern>
    </servlet-mapping>

    <!-- static resources RPC service -->
    <servlet>
        <servlet-name>resourceService</servlet-name>
        <servlet-class>rosa.website.core.server.StaticResourceServiceImpl</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>resourceService</servlet-name>
        <url-pattern>/RosaWebsiteTest/resource</url-pattern>
    </servlet-mapping>

    <!-- Default page to serve -->
    <welcome-file-list>
        <welcome-file>RosaWebsiteTest.html</welcome-file>
    </welcome-file-list>
```

####Configuration properties
In the module's `resources/*/*.client/` directory, a file, _WebsiteConfig.properties_ must be created. It must have,
at a minimum, two properties to configure the web site's history mechanism.

 * _defaultPage_ declares which static HTML page will serve as the home page, to be displayed when there is no
 history token available.
 * _htmlPages_ is a comma delimited list of all of the static HTML pages available in this web application.

```

    defaultPage=one
    htmlPages=one,two,three
```

####Static HTML pages
All of the static HTML pages must be created in a particular directory. All of the pages must appear in `src/main/resources/html`
in order to be loaded by the -core module.

####History Mechanism, using the GWT Activities and Places framework
Create a default history mapper that points to each Place's declared Tokenizer class (each of which will be a
static inner class that implements PlaceTokenizer<T>). The following interface will tell GWT to automatically
generate this default history mapper. The history tokens will follow the scheme: #&lt;PlaceClassName&gt;:&lt;declared_history_token&gt;

```

    @WithTokenizers({
            HTMLPlace.Tokenizer.class,
            CSVDataPlace.Tokenizer.class
    })
    public interface DefaultRosaHistoryMapper extends PlaceHistoryMapper {}
```

The core module already has a concrete history mapper that will accept a list of names, which it will direct to the
HTMLPlace/Activity. This default history mapper will be given to that concrete history mapper to give it fallback
behavior. This will also serve to maintain the default history handling, while also maintaining compatibility with the
old Roman de la Rose history token scheme.

Create an activity mapper that extends the BaseHistoryMapper from the core module. This must be done when/if there
are custom activities/places in this web site, that do not exist in the core module.

####Create a new ClientFactory
If this web application has its own views/activities/places, then a new ClientFactory must be created. This class
must extend ClientFactory from the core module, in order for those factory methods to remain available to the
application.

####Create the GWT web application
Now, you can create the Java class that defines the GWT web application. This class must extend EntryPoint and must
be declared as the entry point in the .gwt.xml file. Here is an example test web application.

```

    import com.google.gwt.activity.shared.ActivityManager;
    import com.google.gwt.activity.shared.ActivityMapper;
    import com.google.gwt.core.client.EntryPoint;
    import com.google.gwt.core.client.GWT;
    import com.google.gwt.dom.client.Style;
    import com.google.gwt.place.shared.Place;
    import com.google.gwt.place.shared.PlaceController;
    import com.google.gwt.place.shared.PlaceHistoryHandler;
    import com.google.gwt.user.client.ui.DockLayoutPanel;
    import com.google.gwt.user.client.ui.RootLayoutPanel;
    import com.google.gwt.user.client.ui.SimplePanel;
    import com.google.web.bindery.event.shared.EventBus;
    import rosa.website.core.client.ClientFactory;
    import rosa.website.core.client.place.HTMLPlace;
    import rosa.website.test.client.nav.DefaultRosaHistoryMapper;
    import rosa.website.core.client.mvp.RosaHistoryMapper;
    import rosa.website.test.client.nav.RosaActivityMapper;

    import java.util.logging.Level;
    import java.util.logging.Logger;

    public class RosaWebsiteTest implements EntryPoint {
        private static final Logger logger = Logger.getLogger("");

        /**
         * This is the default place that will load when the application
         * first starts up. When there is no history token to read, this
         * is the place that will be displayed.
         */
        private Place default_place;
        private String[] htmlPlaces;

        private SimplePanel main_content = new SimplePanel();
        private final DockLayoutPanel main = new DockLayoutPanel(Style.Unit.PX);

        @Override
        public void onModuleLoad() {
            /*
            Using an JS variable called 'config' embedded in the host HTML page:
                var config = {
                    defaultPage: "one",
                    htmlPages: "one,two,three"
                };
            Dictionary config = Dictionary.getDictionary("config");

            final String[] htmlPlaces = config.get("htmlPages").split(",");
            default_place = new HTMLPlace(config.get("defaultPage"));
            */
            init();

            ClientFactory clientFactory = new ClientFactory();
            EventBus eventBus = clientFactory.eventBus();
            final PlaceController placeController = clientFactory.placeController();

            // Start ActivityManager for main widget with ActivityMapper
            ActivityMapper activity_mapper = new RosaActivityMapper(clientFactory);
            final ActivityManager activity_manager = new ActivityManager(activity_mapper, eventBus);
            activity_manager.setDisplay(main_content);

            DefaultRosaHistoryMapper history_mapper = GWT.create(DefaultRosaHistoryMapper.class);
            RosaHistoryMapper appHistoryMapper = new RosaHistoryMapper(history_mapper, htmlPlaces);
            final PlaceHistoryHandler history_handler = new PlaceHistoryHandler(appHistoryMapper);
            history_handler.register(placeController, eventBus, default_place);

            history_handler.handleCurrentHistory();

            main.add(main_content);
            RootLayoutPanel.get().add(main);

            GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
                @Override
                public void onUncaughtException(Throwable e) {
                    logger.log(Level.SEVERE, "Uncaught exception.", e);
                    placeController.goTo(default_place);
                }
            });
        }

        private void init() {
            WebsiteConfig config = WebsiteConfig.INSTANCE;

            htmlPlaces = config.htmlPages().split(",");
            default_place = new HTMLPlace(config.defaultPage());
        }
```

This code does quite a bit.

 * Once the module loads in a web browser, the onModuleLoad method is called.
 * In the init() method, the WebsiteConfig.properties file is read using the GWT Constants interface and the default/starting place
and the list of html pages is configured.
 * The ClientFactory is created. From which, the event bus and place controller are retrieved.
 * The activity mapper is registered with an activity manager.
 * The newly created history mapper is registered as the fallback behavior of the core module's history mapper
 (or custom history mapper that extends it, if custom places exist)
 * The activity manager and history manager have relevant things registered.
 * Content is added to the DOM.
 * The initial history state is handled.
 * An error catch-all is declared in case something gets to this point.
