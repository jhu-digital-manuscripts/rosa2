package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Includes most fields described in the Metadata Requirements tables in the IIIF
 * Presentation API 2.0 specification. All fields listed in this class are at
 * minimum optional fields for all IIIF Presentation API model objects. Several
 * fields from the tables are not included, as they are not allowed in most model
 * objects. Those fields are added only to those relevant objects.
 *
 * http://iiif.io/api/presentation/2.0/#b-summary-of-metadata-requirements
 */
public abstract class PresentationBase implements IIIFNames, Serializable {
    private static final long serialVersionUID = 1L;

    protected String context;
    /**
     * URI identifying this resource.
     */
    protected String id;
    /**
     * Type of resource: Manifest, canvas, image content, etc
     */
    protected String type;
    
    protected ViewingHint viewingHint;
    
    protected ViewingDirection viewingDirection;
    
    // Descriptive Properties
    /**
     * Human readable label, name, or title. Plain text only.
     */
    protected TextValue label;
    /**
     * Long-form prose description, can include some basic HTML formatting.
     */
    protected HtmlValue description;     

    /**
     * URL that should follow the IIIF Image API syntax.
     */
    protected String thumbnailUrl;
    /**
     * Service to extend functionality of thumbnail beyond simply displaying
     * the image. RECOMMENDED: IIIF image service
     */
    protected Service thumbnailService;

    // Rights and Licensing properties
    /**
     * Text to be displayed describing rights or license of a resource. HTML or plain text.
     */
    protected HtmlValue attribution;
    /**
     * URL to license or rights statement. If text is intended to be displayed,
     * use {@link PresentationBase#attribution}
     */
    protected String license;
    /**
     * URL
     */
    protected String logo;

    // Linked properties
    /**
     * URL to external document with further description of the resource. Can be used
     * for search.
     */
    protected String seeAlso;
    /**
     * URL to an external service that extends functionality of this resource.
     */
    protected Service service;
    /**
     * URL to an external resource intended to be displayed.
     */
    protected String relatedUri;
    /**
     * Format, or MIME type, of the related resource.
     */
    protected String relatedFormat;
    /**
     * URI of the resource that contains this resource.
     */
    protected String within;

    /**
     * A list of short descriptive entries, given as pairs of human readable label and
     * value to be displayed to the user. The value should be either simple HTML, including
     * links and text markup, or plain text, and the label should be plain text.
     *
     * This should not be used for discovery purposes. TODO HTML
     */
    protected Map<String, HtmlValue> metadata;

    protected PresentationBase() {
        metadata = new HashMap<>();
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ViewingHint getViewingHint() {
        return viewingHint;
    }

    public void setViewingHint(ViewingHint viewingHint) {
        this.viewingHint = viewingHint;
    }

    public TextValue getLabel() {
        return label;
    }

    public String getLabel(String language) {
        return getLabel() != null ? getLabel().getValue() : "";
    }

    public void setLabel(TextValue label) {
        this.label = label;
    }

    public void setLabel(String label, String language) {
        setLabel(new TextValue(label, language));
    }

    public TextValue getDescription() {
        return description;
    }

    public String getDescription(String language) {
        return getDescription() != null ? getDescription().getValue() : "";
    }

    public void setDescription(HtmlValue description) {
        this.description = description;
    }

    public void setDescription(String description, String language) {
        setDescription(new HtmlValue(description, language));
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Service getThumbnailService() {
        return thumbnailService;
    }

    public void setThumbnailService(Service thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    public TextValue getAttribution() {
        return attribution;
    }

    public String getAttribution(String language) {
        return getAttribution() != null ? getAttribution().getValue() : "";
    }

    public void setAttribution(HtmlValue attribution) {
        this.attribution = attribution;
    }

    public void addAttribution(String attribution, String language) {
        setAttribution(new HtmlValue(attribution, language));
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        this.seeAlso = seeAlso;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getRelatedUri() {
        return relatedUri;
    }

    public void setRelatedUri(String relatedUri) {
        this.relatedUri = relatedUri;
    }

    public String getRelatedFormat() {
        return relatedFormat;
    }

    public void setRelatedFormat(String relatedFormat) {
        this.relatedFormat = relatedFormat;
    }

    public String getWithin() {
        return within;
    }

    public void setWithin(String within) {
        this.within = within;
    }

    public Map<String, HtmlValue> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, HtmlValue> metadata) {
        this.metadata = metadata;
    }
    
    public ViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(ViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    /**
     * Helper for subclasses.
     * 
     * @return hashCode
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribution == null) ? 0 : attribution.hashCode());
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((license == null) ? 0 : license.hashCode());
        result = prime * result + ((logo == null) ? 0 : logo.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((relatedFormat == null) ? 0 : relatedFormat.hashCode());
        result = prime * result + ((relatedUri == null) ? 0 : relatedUri.hashCode());
        result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((thumbnailService == null) ? 0 : thumbnailService.hashCode());
        result = prime * result + ((thumbnailUrl == null) ? 0 : thumbnailUrl.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((viewingDirection == null) ? 0 : viewingDirection.hashCode());
        result = prime * result + ((viewingHint == null) ? 0 : viewingHint.hashCode());
        result = prime * result + ((within == null) ? 0 : within.hashCode());
        return result;
    }

    protected boolean canEqual(Object obj) {
        return (obj instanceof PresentationBase);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PresentationBase))
            return false;
        PresentationBase other = (PresentationBase) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (attribution == null) {
            if (other.attribution != null)
                return false;
        } else if (!attribution.equals(other.attribution))
            return false;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (license == null) {
            if (other.license != null)
                return false;
        } else if (!license.equals(other.license))
            return false;
        if (logo == null) {
            if (other.logo != null)
                return false;
        } else if (!logo.equals(other.logo))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        if (relatedFormat == null) {
            if (other.relatedFormat != null)
                return false;
        } else if (!relatedFormat.equals(other.relatedFormat))
            return false;
        if (relatedUri == null) {
            if (other.relatedUri != null)
                return false;
        } else if (!relatedUri.equals(other.relatedUri))
            return false;
        if (seeAlso == null) {
            if (other.seeAlso != null)
                return false;
        } else if (!seeAlso.equals(other.seeAlso))
            return false;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        if (thumbnailService == null) {
            if (other.thumbnailService != null)
                return false;
        } else if (!thumbnailService.equals(other.thumbnailService))
            return false;
        if (thumbnailUrl == null) {
            if (other.thumbnailUrl != null)
                return false;
        } else if (!thumbnailUrl.equals(other.thumbnailUrl))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (viewingDirection != other.viewingDirection)
            return false;
        if (viewingHint != other.viewingHint)
            return false;
        if (within == null) {
            if (other.within != null)
                return false;
        } else if (!within.equals(other.within))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "PresentationBase [context=" + context + ", id=" + id + ", type=" + type + ", viewingHint="
                + viewingHint + ", viewingDirection=" + viewingDirection + ", label=" + label + ", description="
                + description + ", thumbnailUrl=" + thumbnailUrl + ", thumbnailService=" + thumbnailService
                + ", attribution=" + attribution + ", license=" + license + ", logo=" + logo + ", seeAlso=" + seeAlso
                + ", service=" + service + ", relatedUri=" + relatedUri + ", relatedFormat=" + relatedFormat
                + ", within=" + within + ", metadata=" + metadata + "]";
    }

}
