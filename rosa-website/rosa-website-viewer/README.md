viewer module

  * viewer widgets (by package) _rosa.website.viewer.client._
    * _fsiviewer_ : Wrapper widget for FSI flash viewer
    * _jsviewer_ : Widget for legacy JS viewer from old manuscript sites
    * _pageturner_ : newer page turner widget based on JS viewers provided by FSI
      * contains its own model
      * _FsiPageTurner_ custom class composed of FsiViewer and FsiImageFlow, two wrapper classes for FSI provided viewers
  * associated viewer services
    * Refer to: rosa.gwt.common.client.FSIService, rosa.gwt.common.client.EmbeddedObjectViewer
  * viewer data model?