package rosa.iiif.presentation.model;

public interface IIIFNames {

    String IIIF_PRESENTATION_CONTEXT = "http://iiif.io/api/presentation/2/context.json";
    String IIIF_IMAGE_CONTEXT = "http://iiif.io/api/image/2/context.json";
//    String IIIF_IMAGE_PROFILE_LEVEL2 = "http://iiif.io/api/image/2/profiles/level2.json";
    String IIIF_IMAGE_PROFILE_LEVEL2 = "http://iiif.io/api/image/2/level2.json";

    String IIIF_SEARCH_CONTEXT = "http://iiif.io/api/search/0/context.json";
    String IIIF_SEARCH_PROFILE = "http://iiif.io/api/search/0/search";

    String SC_ANNOTATION_LIST = "sc:AnnotationList";
    String SC_CANVAS = "sc:Canvas";
    String SC_COLLECTION = "sc:Collection";
    String SC_LAYER = "sc:Layer";
    String SC_MANIFEST = "sc:Manifest";
    String SC_RANGE = "sc:Range";
    String SC_SEQUENCE = "sc:Sequence";

    String SC_PAINTING = "sc:painting";
	String OA_COMMENTING = "oa:commenting";    
    String OA_LINKING = "oa:linking";

    String IIIF_IMAGE_API_SELECTOR = "iiif:ImageApiSelector";

    String OA_ANNOTATION = "oa:Annotation";
    String OA_CHOICE = "oa:Choice";
    String OA_CSS_STYLE = "oa:CssStyle";
    String OA_SPECIFIC_RESOURCE = "oa:SpecificResource";
    String OA_SVG_SELECTOR = "oa:SvgSelector";

    String DC_IMAGE = "dctypes:Image";
    String DC_TEXT = "dctypes:Text";

    String CNT_CONTENT_AS_TEXT = "cnt:ContentAsText";
}
