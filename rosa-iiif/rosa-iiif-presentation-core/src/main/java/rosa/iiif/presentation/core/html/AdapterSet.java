package rosa.iiif.presentation.core.html;

import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AdapterSet {

    private Map<Class<?>, AnnotationBaseHtmlAdapter<?>> map;

    @Inject
    public AdapterSet(Set<AnnotationBaseHtmlAdapter<?>> adapters) {
        this.map = new HashMap<>();
        for (AnnotationBaseHtmlAdapter<?> adapter : adapters) {
            map.put(adapter.getAnnotationType(), adapter);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> AnnotationBaseHtmlAdapter<T> get(Class<T> type) {
        return (AnnotationBaseHtmlAdapter<T>) map.get(type);
    }
}
