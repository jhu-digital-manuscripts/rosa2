Contains web site implementations that use the website-core module.

* Roman de la Rose Digital Library web site: rosa-website-rose (rosa-website-impl-rose)
* Christine de Pizan Digital Scriptorium: rosa-website-pizan


Each website that implements the rosa-website-core will have pretty much all of the same functions. For a single collection, 
it will be able to 

* Specify arbitrary static HTML pages to be displayed. History tokens for these pages are defined in `WebsiteConfig.properties`
  and further behavior is extended in the a HistoryConfig class. This Java class provides a central place that loads
  the HTML pages.
* Display various CSVs.
* Browse through data by category.
* Search through data using a full-text search engine.

etc.

## To Implement a New Site

* Create presenters for a header and sidebar. They implement the Header.Presenter or Sidebar.Presenter
* Create GWT Constants interfaces for UI messages and WebsiteConfig properties and ClientBundle for static HTML to be displayed
* Define Activities to match each applicable Place from the core module. These classes define the website behavior in
  response to different places in the site. All will depend on at least WebsiteConfig to define the collection that matters
  in the website implementation.
  
    * BookDescriptionActivity: display description/metadata for a book
    * BookSelectActivity: browse through books by category
    * BookViewActivity: view the book images using built-in JS viewer, or FSIViewer. JSViewerActivity and FSIViewerActivity 
      are both used in the BookViewActivity depending on context. They are not used directly from the Activities and Places 
      framework.
    * HTMLActivity: display HTML
    * CSVDataActivity: display CSV data, depends on website-model to define what CSVs can be displayed
    * SearchActivity: searching, both basic and advanced. Retrieve and display search results
  
* Set up website navigation

    * ActivityMapper: maps specific Place implementations to Activities to execute
    * HistoryMapper: Default history mapper interface will map Place Tokenizers, whereas HistoryMapper implementation
      can be used to map multiple tokens to a single place/activity

* Define website module entry point. Hook up everything here, attach stuff to the module HTML page.

Various resources are needed for the code to work. 

* `WebsiteConfig.properties` under resources.x.client package
* `ModuleName.gwt.xml` under resources
* `fsi-share-map.properties`
* Webapp folder for web.xml, crossdomain.xml, module CSS and module HTML
* HTML pages should be placed somewhere, either in resources or webapp folder

## web.xml
Several items MUST be included in the `web.xml`

* The welcome-file should be set to the GWT module HTML page
* A context-param must be included: archive-path, with a value equal to the desired archive path
* FsiServlet, ArchiveDataServlet, and remote_logging servlet must all be made visible

##Adding new page
A new page in this site needs several classes created before it can be added to the web application.

 * Activity (implements Activity, View.Presenter if applicable) - 
 * Place (extends Place) - new Place contains state information for the app
   * Tokenizer (implements PlaceTokenizer<New_Place>) - easiest to make this a static inner class of the Place
 * View interface (extends IsWidget) - 
 * View implementation (implements View interface) - UI
 
Along with new classes, some changes need to be made to existing places in the code.

 * Activity must be added to the ActivityMapper (RosaActivityMapper)
 * View must be added to the ClientFactory
 * HistoryMapper should be modified if a custom history token scheme is used, otherwise add the Place.Tokenizer to the
   default mapper.