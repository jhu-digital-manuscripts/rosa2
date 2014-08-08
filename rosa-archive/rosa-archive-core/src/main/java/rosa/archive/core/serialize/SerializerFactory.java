package rosa.archive.core.serialize;

/**
 *
 */
public class SerializerFactory {

    public static Serializer get(Class clazz) {

        // TODO this is kinda crappy...
        if (clazz.equals(rosa.archive.model.Book.class)) {
            return new BookSerializer();
        } else if (clazz.equals(rosa.archive.model.BookCollection.class)) {
            return  new BookCollectionSerializer();
        } else if (clazz.equals(rosa.archive.model.BookImage.class)) {
            return new BookImageSerializer();
        } else if (clazz.equals(rosa.archive.model.BookMetadata.class)) {
            return new BookMetadataSerializer();
        } else if (clazz.equals(rosa.archive.model.BookScene.class)) {
            return new BookScenesSerializer();
        } else if (clazz.equals(rosa.archive.model.CharacterNames.class)) {
            return new CharacterNamesSerializer();
        } else if (clazz.equals(rosa.archive.model.ChecksumInfo.class)) {
            return new ChecksumInfoSerializer();
        } else if (clazz.equals(rosa.archive.model.CropInfo.class)) {
            return new CropInfoSerializer();
        } else if (clazz.equals(rosa.archive.model.IllustrationTagging.class)) {
            return new IllustrationTaggingSerializer();
        } else if (clazz.equals(rosa.archive.model.IllustrationTitles.class)) {
            return new IllustrationTitlesSerializer();
        } else if (clazz.equals(rosa.archive.model.NarrativeSections.class)) {
            return new NarrativeSectionsSerializer();
        }

        return null;
    }

}
