package rosa.iiif.presentation.model;

/**
 * {@link TextValue} with the added rule
 * that the value is to be taken as arbitrary HTML that is sanitized. The
 * code that uses this class is responsible for ensuring that the HTML
 * value is safe to use.
 *
 * In following the
 * <a href="http://iiif.io/api/presentation/2.0/#property-values-in-html">IIIF Presentation API v2.0</a>,
 * ONLY the following tags are allowed: &lt;a&gt;, &lt;b&gt;, &lt;br&gt;,
 * &lt;i&gt;, &lt;img&gt;, &lt;p&gt;, &lt;span&gt;
 *
 * ONLY the 'href' attribute is allowed for the &lt;a&gt; tag, and ONLY the 'src' and 'alt'
 * attributes are allowed for the &lt;img&gt; tag. All other attributes and
 * all other elements will be removed.
 */
public class HtmlValue extends TextValue {
    private final String safeValue;

    public HtmlValue(String value, String language) {
        super(value, language);
//        safeValue = IiifHtmlSanitizer.defaultSanitizer().sanitize(value);
        safeValue = value;
    }

    public HtmlValue(String value) {
        this(value, "en");
    }

    @Override
    public String getValue() {
        return safeValue;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    public String asString() {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        HtmlValue htmlValue = (HtmlValue) o;

        if (safeValue != null ? !safeValue.equals(htmlValue.safeValue) : htmlValue.safeValue != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (safeValue != null ? safeValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HtmlValue{" +
                "safeValue='" + safeValue + '\'' +
                '}';
    }
}
