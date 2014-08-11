package rosa.archive.core.serialize;

/**
 *
 */
public class SerializerFactory {

    /**
     * Get the serializer for a data model class.
     *
     * @param clazz
     *          class instance
     * @param <T>
     *          type
     * @return
     *          serializer for specified data model class
     */
    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> serializer(Class clazz) throws SerializerException {
        String[] fullName = clazz.getName().split("\\.");
        String serializerName = "rosa.archive.core.serialize."
                + fullName[fullName.length - 1]
                + "Serializer";
        try {

            Class serializerClass = Class.forName(serializerName);
            return (Serializer<T>) serializerClass.newInstance();

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
            throw new SerializerException("Unable to get serializer.", e);
        }
    }

}
