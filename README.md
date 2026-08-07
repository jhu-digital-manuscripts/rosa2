# Rosa

Rosa is a framework for creating a web application that allows a user to interact with digitized books and complicated metadata. Raw data is stored in a file system.
A command line tool manages the data in the archive and processes it to create static files which will be used by the website. The digitized books are modeled using the IIIF Presentation API 3.
There is also a custom search service based on Amazon Opensearch.
The website uses the IIIF viewer mirador. The viewer has custom plugins.

The Java command line tool is in rosa-tool.
The  Javascript viwer is in rosa-viewer.