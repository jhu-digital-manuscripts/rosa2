package rosa.iiif.presentation.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.BookImage;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.model.SearchOptions;

/**
 * Handle mapping of archive objects into URIs for IIIF Presentation API and
 * Image PAI.
 * 
 * The general structure for presentation API is is COLLECTION / NAME? / NAME? /
 * TYPE
 * 
 * 
 * An image id is Collection Id '/' Book Id '/' Image Id (without extension).
 * Some images may also have a cropped version with a 'cropped' path segment
 * before the Image Id.
 */
public class PresentationUris {
    private final IIIFPresentationRequestFormatter presFormatter;
    private final IIIFRequestFormatter imageFormatter;
    private final StaticResourceRequestFormatter staticFormatter;
    
    private static final ArchiveNameParser nameParser = new ArchiveNameParser();

    public PresentationUris(IIIFPresentationRequestFormatter presFormatter, IIIFRequestFormatter imageFormatter,
                            StaticResourceRequestFormatter staticFormatter) {
        this.presFormatter = presFormatter;
        this.imageFormatter = imageFormatter;
        this.staticFormatter = staticFormatter;
    }

    public String getCollectionURI(String collection) {
        return presFormatter.format(new PresentationRequest(PresentationRequestType.COLLECTION, collection));
    }

    public String getManifestURI(String collection, String book) {
        return presFormatter.format(new PresentationRequest(PresentationRequestType.MANIFEST, collection, book));
    }

    /**
     * The Annotation name must be unique for the given image.
     * 
     * @param collection
     * @param book
     * @param image
     * @param name
     * @return URI
     */
    public String getAnnotationURI(String collection, String book, BookImage image, String name) {
    	return getInflectedURI(PresentationRequestType.ANNOTATION, collection, book, nameParser.shortUniqueImageIdInBook(image.getId()), name);
    }

    public String getAnnotationListURI(String collection, String book, BookImage image, AnnotationListType type) {
    	return getInflectedURI(PresentationRequestType.ANNOTATION_LIST, collection, book, nameParser.shortUniqueImageIdInBook(image.getId()), type.name().toLowerCase());
    }
 
    public String getSequenceURI(String collection, String book, String name) {
        return presFormatter.format(new PresentationRequest(PresentationRequestType.SEQUENCE, collection, book, name));
    }

    public String getRangeURI(String collection, String book, String name) {
        return presFormatter.format(new PresentationRequest(PresentationRequestType.RANGE, collection, book, name));
    }

    public String getCanvasURI(String collection, String book, BookImage image) {
        return presFormatter.format(new PresentationRequest(PresentationRequestType.CANVAS, collection, book, nameParser.shortUniqueImageIdInBook(image.getId())));
    }

    public String getImageServiceURI(String collection, String book, String imageId, boolean cropped) {
        return imageFormatter.format(get_iiif_image_id(collection, book, imageId, cropped));
    }
    
    public String getJpegURI(String collection, String book, String imageId, boolean cropped) {
        return imageFormatter.format(get_iiif_image_id(collection, book, imageId, cropped)) + "/full/full/0/default.jpg";
    }
    
    public String getThumbnailImageURI(String collection, String book, String imageId, boolean cropped) {
        return imageFormatter.format(get_iiif_image_id(collection, book, imageId, cropped));
    }
    
    private String getInflectedURI(PresentationRequestType type, String collection, String book, String name, String inflection) {
    	if (inflection == null) {
    		inflection = "";
    	}
    	
    	return presFormatter.format(new PresentationRequest(type, collection, book, name + ";" + inflection));
    }
    
    /**
     * Split an inflected URI name into name and inflection portions.
     * 
     * @param name
     * @return [name, inflection or ""]
     */
    public String[] splitInflectedName(String name) {
    	String inflection = "";
    	
    	int i = name.lastIndexOf(';');
    	
    	if (i != -1) {
    		if (i < name.length() - 2) {
        		inflection = name.substring(i + 1);    			
    		}
    		
    		name = name.substring(0, i);
    	}
    	
    	return new String[] {name, inflection};
    }

    private String get_iiif_image_id(String collection, String book, String imageId, boolean cropped) {
        String image = imageId;
        int i = image.lastIndexOf('.');

        if (i > 0) {
            image = image.substring(0, i);
        }

        return collection + (book == null ? "" : "/" + book) + "/" + (cropped ? "cropped/" : "") + image;
    }

    public String getJHSearchURI(PresentationRequest req, String query, SearchOptions opts, String categories) {
        StringBuilder url = new StringBuilder(presFormatter.format(req));

        try {
            url.append(JHSearchService.RESOURCE_PATH).append('?').append(JHSearchService.QUERY_PARAM).append('=')
                    .append(URLEncoder.encode(query, "UTF-8")).append('&').append(JHSearchService.MAX_MATCHES_PARAM)
                    .append('=').append(opts.getMatchCount());

            if (opts.getSortOrder() != null) {
                url.append('&').append(JHSearchService.SORT_ORDER_PARAM).append('=')
                        .append(URLEncoder.encode(opts.getSortOrder().name().toLowerCase(), "UTF-8"));
            }

            url.append('&').append(JHSearchService.OFFSET_PARAM).append('=').append(opts.getOffset());

            if (categories != null && !categories.equals("")) {
                url.append('&').append(JHSearchService.CATEGORIES).append('=')
                        .append(URLEncoder.encode(categories, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return url.toString();
    }

    public String getStaticResourceUri(String collection, String book, String target) {
        return staticFormatter.format(collection, book, target);
    }
}
