package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.iiif.presentation.core.transform.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransformerSet {
    private final Map<Class<?>, Transformer<?>> map;

    @Inject
    public TransformerSet(Set<Transformer<?>> set) {
        map = new HashMap<>();

        for (Transformer<?> t : set) {
            map.put(t.getType(), t);
        }
    }

    /**
     * @param type
     * @return transformer for type, or NULL if it does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> Transformer<T> getTransformer(Class<T> type) {
        return (Transformer<T>) map.get(type);
    }
}
