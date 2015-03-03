core module

  * basic navigation?
  * history handling? (provide activities/places)
    * See rosa.gwt.common.client.Action (for Roman de la Rose website. There is an equivalent for Pizan)
  * some default widgets to hold data?
    * CSV display: columns must be sortable, provide link to a Google Doc

  * load data from archive
  * adapt to website model (CSVs)
    * Use apache commons CSV? custom CSV parsing in the archive core
    * Website model based off old idea of pre-prepared CSVs located in the WAR?
    * When to prepare the CSV data, build time? runtime on init?
  * Use Guice in server code, GIN in client code

