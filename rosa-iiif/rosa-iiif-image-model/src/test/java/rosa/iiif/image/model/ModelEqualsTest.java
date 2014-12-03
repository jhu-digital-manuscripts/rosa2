package rosa.iiif.image.model;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ModelEqualsTest {
    @Test
    public void testImageInfo() {
        EqualsVerifier.forClass(ImageInfo.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testImageRequest() {
        EqualsVerifier.forClass(ImageRequest.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testImageServerProfile() {
        EqualsVerifier.forClass(ImageServerProfile.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testInfoRequest() {
        EqualsVerifier.forClass(InfoRequest.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testRegion() {
        EqualsVerifier.forClass(Region.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testRotation() {
        EqualsVerifier.forClass(Rotation.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testServiceReference() {
        EqualsVerifier.forClass(ServiceReference.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testSize() {
        EqualsVerifier.forClass(Size.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testTileInfo() {
        EqualsVerifier.forClass(TileInfo.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
}
