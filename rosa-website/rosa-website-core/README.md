core module

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

**ArchiveDataService**: load data from the archive into the website. Any call to this service from the website
should cache the results. Use this service to front load data, store the website data in memory (or on file?)
on website initialization to avoid dependency on the archive?

  * Copy things into the webapp to maintain proper Google crawlability?
  * BookDataCSV, CollectionCSV, IllustrationTitleCSV: pre-formatted CSVs to be displayed on screen
    as is. These should be available somewhere for download.
  * BookCollection, Book: load appropriate archive object on demand.



For mapping the same Place to multiple URL fragment prefixes:
<http://stackoverflow.com/questions/10089964/places-that-share-all-but-prefix-or-how-to-use-placehistorymapperwithfactory>