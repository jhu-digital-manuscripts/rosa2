package rosa.archive.core.serialize;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

/**
 * Manage a set of serializers which can be looked up by the type of the object
 * they serialize.
 */
public class SerializerSet {
    private Map<Class<?>, Serializer<?>> map;

    @Inject
    public SerializerSet(Set<Serializer<?>> serializers) {
        this.map = new HashMap<Class<?>, Serializer<?>>();

        for (Serializer<?> s : serializers) {
            map.put(s.getObjectType(), s);
        }
    }
    
    /**
     * @param type
     * @return Serializer for type or null if it does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> getSerializer(Class<T> type) {
        return (Serializer<T>) map.get(type);
    }
}
