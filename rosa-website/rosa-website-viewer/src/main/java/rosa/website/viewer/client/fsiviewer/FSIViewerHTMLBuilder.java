package rosa.website.viewer.client.fsiviewer;

import com.google.gwt.http.client.URL;

public class FSIViewerHTMLBuilder {
    private static final String PLUGINSPAGE = "http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash";
    private static final String FLASH_CODEBASE = "http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,65,0";
    private static final String MIME_TYPE = "application/x-shockwave-flash";
    private static final String DEFAULT_CLASSID = "clsid:D27CDB6E-AE6D-11cf-96B8-444553540000";

    private FSIViewerType type;

    private String fsi_server_url;
    private String fsi_doc_path;

    private String collection;
    private String book;
    private String language_code;
    private String viewer_width;
    private String viewer_height;

    private int tile_width;
    private int tile_height;
    private int initial_image_index; // FSI image index starts at 1

    private boolean debug;

    /**
     * Create a new FsiViewerHTMLBuilder.
     */
    public FSIViewerHTMLBuilder() {
        viewer_height = "600";
        viewer_width = "400";
        tile_width = 600;
        tile_height = 600;
        initial_image_index = 1;
        debug = false;
    }

    /**
     * @param type type of viewer to build (showcase, pages)
     * @return this builder
     */
    public FSIViewerHTMLBuilder type(FSIViewerType type) {
        this.type = type;
        return this;
    }

    /**
     * @param fsi_server_url URL to the FSI image server
     * @return this builder
     */
    public FSIViewerHTMLBuilder fsiServerUrl(String fsi_server_url) {
        this.fsi_server_url = fsi_server_url;
        return this;
    }

    /**
     * @param fsi_doc_path URL pointing to a book specific FSI configuration document
     * @return this builder
     */
    public FSIViewerHTMLBuilder fsiBookData(String fsi_doc_path) {
        this.fsi_doc_path = fsi_doc_path;
        return this;
    }

    /**
     * @param collection name of collection
     * @param book name of book
     * @param languageCode language code to display text
     * @return this builder
     */
    public FSIViewerHTMLBuilder book(String collection, String book, String languageCode) {
        this.collection = collection;
        this.book = book;
        this.language_code = languageCode;
        return this;
    }

    /**
     * @param width width of viewer
     * @param height height of viewer
     * @return this builder
     */
    public FSIViewerHTMLBuilder viewerSize(String width, String height) {
        this.viewer_width = width;
        this.viewer_height = height;
        return this;
    }

    /**
     * @param width tile width in pixels
     * @param height tile height in pixels
     * @return this builder
     */
    public FSIViewerHTMLBuilder tileSize(int width, int height) {
        this.tile_width = width;
        this.tile_height = height;
        return this;
    }

    /**
     * @param index index of first image to display
     * @return this builder
     */
    public FSIViewerHTMLBuilder initialImage(int index) {
        this.initial_image_index = index + 1;
        return this;
    }

    /**
     * @param debug debug mode on/off
     * @return this builder
     */
    public FSIViewerHTMLBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * This build will result in invalid HTML if the following have
     * not been specified: book, language code, viewer type, fsi_doc_path
     * (URL to XML config for the viewer)
     *
     * @return HTML for an embedded FSI flash viewer
     */
    public String build() {
        String url = fsi_server_url + "/viewer/fsi.swf?" + urlParameters();

        return "<OBJECT "
                + "id=\"" + type.getViewerId() + "\" "
                + "CLASSID=\"" + DEFAULT_CLASSID + "\" "
                + "CODEBASE=\"" + FLASH_CODEBASE + "\" "
                + "WIDTH=\"" + viewer_width + "\" "
                + "HEIGHT=\"" + viewer_height + "\">"           // end OBJECT open tag
                + viewerParameters(url)
                + "<EMBED NAME=\"" + type.getViewerId() + "\" "
                + "swliveconnect=\"true\" allowscriptaccess=\"always\" allowfullscreen=\"true\" "
                + "SRC=\"" + url + "\" "
                + "MENU=\"false\" wmode=\"opaque\" PLUGINSPAGE=\"" + PLUGINSPAGE + "\" "
                + "WIDTH=\"" + viewer_width + "\" "
                + "HEIGHT=\"" + viewer_height + "\" "
                + "TYPE=\"" + MIME_TYPE + "\">"                 // end EMBED start tag
                + "</EMBED>"                                    //     EMBED close tag
                + "</OBJECT>";                                  //     OBJECT close tag
    }

    private String urlParameters() {
        StringBuilder url_sb = new StringBuilder();

        url_sb.append("cfg=");
        url_sb.append(URL.encode(fsi_doc_path));
        url_sb.append(langparam(language_code));
        url_sb.append("&TileSizeX=");
        url_sb.append(tile_width);
        url_sb.append("&TileSizeY=");
        url_sb.append(tile_height);
        url_sb.append("&InitialMouseMode=");
        url_sb.append(0);                               // Set initial mouse mode to PAN
        url_sb.append("&debug=");
        url_sb.append(debug ? 1 : 0);                   // DEBUG == true (debug=1) ELSE (debug=0)

        if (collection != null && book != null) {
            url_sb.append("&plugins=notepad");
            url_sb.append("&notepad_UniqueID=");
            url_sb.append(type.getViewerId());
            url_sb.append('.');
            url_sb.append(collection);
            url_sb.append('.');
            url_sb.append(book);
        }

        // SHOWCASE = default case
        switch (type) {
            case PAGES:
                url_sb.append("&pages_PageNumbers=false");
                url_sb.append("&pages_Events=true");
                url_sb.append("&pages_ForceInitialPage=");
                break;
            default:
                url_sb.append("&showcase_InitialImage=");
                break;
        }
        url_sb.append(initial_image_index);

        return url_sb.toString();
    }

    private String langparam(String lc) {
        if (lc == null) {
            lc = "en";
        }

        // "en" is default
        switch (lc) {
            case "fr":
                return "&language=french";
            default:
                return "&language=english";
        }
    }

    private String viewerParameters(String url) {
        return "<PARAM NAME=\"menu\" VALUE=\"false\">"
                + "<PARAM NAME=\"wmode\" VALUE=\"opaque\">"
                + "<PARAM NAME=\"swliveconnect\" VALUE=\"true\">"
                + "<PARAM NAME=\"allowscriptaccess\" VALUE=\"always\">"
                + "<PARAM NAME=\"allowfullscreen\" VALUE=\"true\">"
                + "<PARAM NAME=\"quality\" VALUE=\"high\">"
                + "<PARAM NAME=\"movie\" VALUE=\"" + url + "\">";
    }

}
