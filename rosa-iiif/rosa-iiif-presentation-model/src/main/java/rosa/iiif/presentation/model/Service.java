package rosa.iiif.presentation.model;

import java.io.Serializable;

public class Service implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String context;
    protected String id;
    protected String profile;
    protected String label;

    public Service() {
    }

    public Service(String context, String id, String profile) {
        this.context = context;
        this.id = id;
        this.profile = profile;
    }

    public Service(String context, String id, String profile, String label) {
        this.context = context;
        this.id = id;
        this.profile = profile;
        this.label = label;
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

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (profile != null ? profile.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    protected boolean canEqual(Object obj) {
        return (obj instanceof Service);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) return false;

        Service service = (Service) o;
        if (!service.canEqual(this)) return false;
        if (context != null ? !context.equals(service.context) : service.context != null) return false;
        if (id != null ? !id.equals(service.id) : service.id != null) return false;
        if (profile != null ? !profile.equals(service.profile) : service.profile != null) return false;
        return label != null ? label.equals(service.label) : service.label == null;
    }

    @Override
    public String toString() {
        return "Service [context=" + context + ", id=" + id + ", profile=" + profile + "]";
    }
}
