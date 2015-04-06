package rosa.archive.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

/**
 * Testing equals and hashcode of model objects
 */
public class ModelEqualsAndHashCodeTest {


    @Test
    public void bookImageTest() {
        EqualsVerifier
                .forClass(BookImage.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookMetadataTest() {
        EqualsVerifier
                .forClass(BookMetadata.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookSceneTest() {
        EqualsVerifier
                .forClass(BookScene.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookStructureTest() {
        EqualsVerifier
                .forClass(BookStructure.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookTextTest() {
        EqualsVerifier
                .forClass(BookText.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void characterNamesTest() {
        EqualsVerifier
                .forClass(CharacterNames.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void characterNameTest() {
        EqualsVerifier
                .forClass(CharacterName.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void cropDataTest() {
        EqualsVerifier
                .forClass(CropData.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void cropInfoTest() {
        EqualsVerifier
                .forClass(CropInfo.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void illustrationTest() {
        EqualsVerifier
                .forClass(Illustration.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void illustrationTaggingTest() {
        EqualsVerifier
                .forClass(IllustrationTagging.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void illustrationTitlesTest() {
        EqualsVerifier
                .forClass(IllustrationTitles.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void imageListTest() {
        EqualsVerifier
                .forClass(ImageList.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void narrativeSceneTest() {
        EqualsVerifier
                .forClass(NarrativeScene.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void narrativeSectionsTest() {
        EqualsVerifier
                .forClass(NarrativeSections.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void narrativeTaggingTest() {
        EqualsVerifier
                .forClass(NarrativeTagging.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void permissionTest() {
        EqualsVerifier
                .forClass(Permission.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void sha1sumTest() {
        EqualsVerifier
                .forClass(SHA1Checksum.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void transcriptionTest() {
        EqualsVerifier
                .forClass(Transcription.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void referenceSheetTest() {
        EqualsVerifier
                .forClass(ReferenceSheet.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookReferenceSheetTest() {
        EqualsVerifier
                .forClass(BookReferenceSheet.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void fileMapTest() {
        EqualsVerifier
                .forClass(FileMap.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookDescriptionTest() {
        EqualsVerifier
                .forClass(BookDescription.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookTest() {
        EqualsVerifier
                .forClass(Book.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void bookCollectionTest() {
        EqualsVerifier
                .forClass(BookCollection.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

}
