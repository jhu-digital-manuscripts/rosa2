package rosa.website.core.client;

public class AppContext {
    private boolean useFlash;
    private String language;

    public AppContext() {
        useFlash = true;
        language = "en";
    }

    public boolean useFlash() {
        return useFlash;
    }

    void setUseFlash(boolean useFlash) {
        this.useFlash = useFlash;
    }

    public String getLanguage() {
        return language;
    }

    void setLanguage(String language) {
        this.language = language;
    }
}
