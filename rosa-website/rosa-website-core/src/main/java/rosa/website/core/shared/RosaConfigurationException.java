package rosa.website.core.shared;

public class RosaConfigurationException extends RuntimeException {

    private String propertyName;
    private String value;

    public RosaConfigurationException() {
        super();
        this.propertyName = null;
        this.value = null;
    }

    public RosaConfigurationException(String propertyName, String value) {
        this("Misconfigured property [" + propertyName + ": " + value + "]", propertyName, value);
    }

    public RosaConfigurationException(String message, String propertyName, String value) {
        super(message);
        this.propertyName = propertyName;
        this.value = value;
    }

    public RosaConfigurationException(String message, Throwable cause, String propertyName, String value) {
        super(message, cause);
        this.propertyName = propertyName;
        this.value = value;
    }

    public RosaConfigurationException(Throwable cause, String propertyName, String value) {
        this("Misconfigured property [" + propertyName + ": " + value + "]", cause, propertyName, value);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getValue() {
        return value;
    }
}
