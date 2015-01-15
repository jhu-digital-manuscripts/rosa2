package rosa.iiif.presentation.model;

import java.io.Serializable;

public class Service implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String context;
    protected String id;
    protected String profile;

    public Service() {
    }

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((profile == null) ? 0 : profile.hashCode());
        return result;
    }
    
    protected boolean canEqual(Object obj) {
        return (obj instanceof Service);
    }
    
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Service))
            return false;
        
        Service other = (Service) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (profile == null) {
            if (other.profile != null)
                return false;
        } else if (!profile.equals(other.profile))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Service [context=" + context + ", id=" + id + ", profile=" + profile + "]";
    }
}
