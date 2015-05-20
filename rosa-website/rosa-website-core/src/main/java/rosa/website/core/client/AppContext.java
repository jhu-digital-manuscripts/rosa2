package rosa.website.core.client;

public class AppContext {
    private boolean useFlash;
    private String collection;

    public AppContext() {
        useFlash = true;
    }

    public boolean useFlash() {
        return useFlash;
    }

    public void setUseFlash(boolean useFlash) {
        this.useFlash = useFlash;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }
}
