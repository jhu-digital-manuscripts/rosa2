package rosa.website.core.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class LanguageChangeEvent extends GwtEvent<LangaugeChangeEventHandler> {
    public static Type<LangaugeChangeEventHandler> TYPE = new Type<>();

    private final String language;

    public LanguageChangeEvent(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public Type<LangaugeChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(LangaugeChangeEventHandler handler) {
        handler.onLanguageChange(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LanguageChangeEvent that = (LanguageChangeEvent) o;

        return !(language != null ? !language.equals(that.language) : that.language != null);

    }

    @Override
    public int hashCode() {
        return language != null ? language.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "LanguageChangeEvent{" +
                "language='" + language + '\'' +
                '}';
    }
}
