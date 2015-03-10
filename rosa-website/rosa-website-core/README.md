#core module

  * basic navigation?
  * history handling? (provide activities/places)
    * See rosa.gwt.common.client.Action (for Roman de la Rose website. There is an equivalent for Pizan)
  * some default widgets to hold data?
    * CSV display: columns must be sortable, provide link to a Google Doc

  * load data from archive (archive data service)
  * adapt to website model (CSVs)
    * Use apache commons CSV? custom CSV parsing in the archive core
    * Website model based off old idea of pre-prepared CSVs located in the WAR?
    * When to prepare the CSV data, build time? runtime on init?
  * Use Guice in server code, GIN in client code

##ArchiveDataService:
load data from the archive into the website. Any call to this service from the website
should cache the results. Use this service to front load data, store the website data in memory (or on file?)
on website initialization to avoid dependency on the archive?

  * Copy things into the webapp to maintain proper Google crawlability?
  * BookDataCSV, CollectionCSV, IllustrationTitleCSV: pre-formatted CSVs to be displayed on screen
    as is. These should be available somewhere for download.
  * BookCollection, Book: load appropriate archive object on demand.



For mapping the same Place to multiple URL fragment prefixes:
<http://stackoverflow.com/questions/10089964/places-that-share-all-but-prefix-or-how-to-use-placehistorymapperwithfactory>


##Basic navigation and history handling
Some pages are shared between the Rose and Pizan web sites, and will be necessary for future web sites.
They can be declared in the core and used in the web site modules.

  * Define: views, presenters, activities, places
  * GWT UIBinder for views?
  * GIN for DI
  * Navigation must keep old URL schemes. Fortunately, each web site "Action" seems to have a set prefix
  * Places needed:
    * Display collection data as CSV (collection data, collection book data, illustration titles)
    * Be able to display arbitrary number of static HTML pages in main content area

####Rose Actions
  * HOME("home") - static page
  * SEARCH("search")
  * BROWSE_BOOK("browse")
  * SELECT_BOOK("select")
  * READ_BOOK("read")
  * VIEW_BOOK("book")
  * VIEW_NARRATIVE_SECTIONS("sections") - CSV view
  * VIEW_PARTNERS("partners") - static page
  * VIEW_ROSE_HISTORY("rose") - static page
  * VIEW_PROJECT_HISTORY("project") - static page
  * VIEW_CONTACT("contact") - static page
  * VIEW_CORPUS("corpus") - CSV view
  * VIEW_TERMS("terms") - static page
  * VIEW_DONATION("donation") - static page
  * VIEW_COLLECTION_DATA("data") - CSV view
  * VIEW_CHARACTER_NAMES("chars") - CSV view
  * VIEW_ILLUSTRATION_TITLES("illustrations") - CSV view
  * VIEW_BOOK_BIB("bib") ??

####Pizan Actions
  * HOME("home") - static page
  * SEARCH("search")
  * BROWSE_BOOK("browse")
  * SELECT_BOOK("select")
  * READ_BOOK("read")
  * VIEW_BOOK("book")
  * VIEW_PARTNERS("partners") - static page
  * VIEW_PIZAN("pizan") - static page
  * VIEW_CONTACT("contact") - static page
  * VIEW_WORKS("works") - CSV view
  * VIEW_TERMS("terms") - static page
  * VIEW_PROPER_NAMES("names") - static page