//package rosa.archive.core.serialize;
//
//import org.junit.Test;
//import rosa.archive.model.BookMetadata;
//import rosa.archive.model.BookStructure;
//import rosa.archive.model.CharacterNames;
//import rosa.archive.model.ChecksumInfo;
//import rosa.archive.model.CropInfo;
//import rosa.archive.model.IllustrationTagging;
//import rosa.archive.model.IllustrationTitles;
//import rosa.archive.model.ImageList;
//import rosa.archive.model.MissingList;
//import rosa.archive.model.NarrativeSections;
//import rosa.archive.model.NarrativeTagging;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//
///**
// *
// */
//public class SerializerFactoryTest {
//
//    @Test
//    public void imageListTest() throws Exception {
//        Serializer<ImageList> bis = SerializerFactory.serializer(ImageList.class);
//        assertNotNull(bis);
//        assertEquals(ImageListSerializer.class, bis.getClass());
//    }
//
//    @Test
//    public void bookMetadataTest() throws Exception {
//        Serializer<BookMetadata> serializer = SerializerFactory.serializer(BookMetadata.class);
//        assertNotNull(serializer);
//        assertEquals(BookMetadataSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void bookStructureTest() throws Exception {
//        Serializer<BookStructure> serializer = SerializerFactory.serializer(BookStructure.class);
//        assertNotNull(serializer);
//        assertEquals(BookStructureSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void characterNamesTest() throws Exception {
//        Serializer<CharacterNames> serializer = SerializerFactory.serializer(CharacterNames.class);
//        assertNotNull(serializer);
//        assertEquals(CharacterNamesSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void checksumInfoTest() throws Exception {
//        Serializer<ChecksumInfo> serializer = SerializerFactory.serializer(ChecksumInfo.class);
//        assertNotNull(serializer);
//        assertEquals(ChecksumInfoSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void cropInfoTest() throws Exception {
//        Serializer<CropInfo> serializer = SerializerFactory.serializer(CropInfo.class);
//        assertNotNull(serializer);
//        assertEquals(CropInfoSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void illustrationTaggingTest() throws Exception {
//        Serializer<IllustrationTagging> serializer = SerializerFactory.serializer(IllustrationTagging.class);
//        assertNotNull(serializer);
//        assertEquals(IllustrationTaggingSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void illustrationTitlesTest() throws Exception {
//        Serializer<IllustrationTitles> serializer = SerializerFactory.serializer(IllustrationTitles.class);
//        assertNotNull(serializer);
//        assertEquals(IllustrationTitlesSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void missingListTest() throws Exception {
//        Serializer<MissingList> serializer = SerializerFactory.serializer(MissingList.class);
//        assertNotNull(serializer);
//        assertEquals(MissingListSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void narrativeSectionsTest() throws Exception {
//        Serializer<NarrativeSections> serializer = SerializerFactory.serializer(NarrativeSections.class);
//        assertNotNull(serializer);
//        assertEquals(NarrativeSectionsSerializer.class, serializer.getClass());
//    }
//
//    @Test
//    public void narrativeTaggingTest() throws Exception {
//        Serializer<NarrativeTagging> serializer = SerializerFactory.serializer(NarrativeTagging.class);
//        assertNotNull(serializer);
//        assertEquals(NarrativeTaggingSerializer.class, serializer.getClass());
//    }
//
//}
