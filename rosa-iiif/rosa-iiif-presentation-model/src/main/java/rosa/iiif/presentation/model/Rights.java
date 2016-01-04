package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains rights and license information.
 */
public class Rights implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Text that must be shown when the image is displayed or used. It might include copyright
     * or ownership statements, or a simple acknowledgement of the providing institution. The
     * value may contain simple HTML, using only the a, b, br, i, img, p and span tags, as
     * described in the HTML Markup in Property Values section of the Presentation API.
     */
    private Map<String, HtmlValue> attributionMap;

    /**
     * One or more URLs of an external resource that describes the license or rights statement
     * under which the image may be used.
     */
    private String[] licenseUris;

    /**
     * One or more URLs of a small image that represents an individual or organization associated
     * with the image service. The logo image must be clearly rendered when the main image is
     * displayed or used. Clients must not crop, rotate, or otherwise distort the image
     */
    private String[] logoUris;

    /**
     * An image service reference, if available. It is recommended that this service be a IIIF
     * image service.
     */
    private Service logoService;

    public Rights() {
        this.attributionMap = new HashMap<>();
    }

    public void setAttributionMap(Map<String, HtmlValue> attributionMap) {
        this.attributionMap = attributionMap;
    }

    public Map<String, HtmlValue> getAttributionMap() {
        return attributionMap;
    }

    public boolean hasAttribution() {
        return attributionMap != null && !attributionMap.isEmpty();
    }

    public boolean hasAttribution(String lang) {
        return hasAttribution() && attributionMap.containsKey(lang);
    }

    public void addAttribution(String text, String lang) {
        attributionMap.put(lang, new HtmlValue(text, lang));
    }

    public String getAttribution(String lang) {
        return attributionMap.get(lang).getValue();
    }

    public String[] getLicenseUris() {
        return licenseUris;
    }

    public boolean hasOneLicense() {
        return licenseUris != null && licenseUris.length == 1;
    }

    public boolean hasMultipleLicenses() {
        return licenseUris != null && licenseUris.length > 1;
    }

    public void setLicenseUris(String[] licenseUris) {
        this.licenseUris = licenseUris;
    }

    public String getFirstLicense() {
        return isEmpty(licenseUris) ? null : licenseUris[0];
    }

    public String[] getLogoUris() {
        return logoUris;
    }

    public void setLogoUris(String[] logoUris) {
        this.logoUris = logoUris;
    }

    public String getFirstLogo() {
        return isEmpty(logoUris) ? null : logoUris[0];
    }

    public boolean hasOneLogo() {
        return logoUris != null && logoUris.length == 1;
    }

    public boolean hasMultipleLogos() {
        return logoUris != null && logoUris.length > 1;
    }

    public Service getLogoService() {
        return logoService;
    }

    public void setLogoService(Service logoService) {
        this.logoService = logoService;
    }

    public boolean hasLogoService() {
        return logoService != null;
    }

    private boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rights)) return false;

        Rights rights = (Rights) o;

        if (attributionMap != null ? !attributionMap.equals(rights.attributionMap) : rights.attributionMap != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(licenseUris, rights.licenseUris)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(logoUris, rights.logoUris)) return false;
        return !(logoService != null ? !logoService.equals(rights.logoService) : rights.logoService != null);
    }

    @Override
    public int hashCode() {
        int result = attributionMap != null ? attributionMap.hashCode() : 0;
        result = 31 * result + (licenseUris != null ? Arrays.hashCode(licenseUris) : 0);
        result = 31 * result + (logoUris != null ? Arrays.hashCode(logoUris) : 0);
        result = 31 * result + (logoService != null ? logoService.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Rights{attributionMap='" + attributionMap + "', licenseUris=" + Arrays.toString(licenseUris) +
                ", logoUris=" + Arrays.toString(logoUris) + "logoService=" + logoService + '}';
    }
}
