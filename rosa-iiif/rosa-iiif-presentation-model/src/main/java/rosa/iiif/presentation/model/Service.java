package rosa.iiif.presentation.model;

import java.io.Serializable;

public class Service implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String context;
    protected String id;
    protected String profile;

    public Service() {}

    public Service(String context, String id, String profile) {
        this.context = context;
        this.id = id;
        this.profile = profile;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        if (context != null ? !context.equals(service.context) : service.context != null) return false;
        if (id != null ? !id.equals(service.id) : service.id != null) return false;
        if (profile != null ? !profile.equals(service.profile) : service.profile != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (profile != null ? profile.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Service{" +
                "context='" + context + '\'' +
                ", id='" + id + '\'' +
                ", profile='" + profile + '\'' +
                '}';
    }
}
