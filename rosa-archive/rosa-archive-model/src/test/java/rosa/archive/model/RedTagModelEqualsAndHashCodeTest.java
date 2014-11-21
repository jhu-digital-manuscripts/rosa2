package rosa.archive.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import rosa.archive.model.redtag.Blank;
import rosa.archive.model.redtag.Heading;
import rosa.archive.model.redtag.Image;
import rosa.archive.model.redtag.Initial;
import rosa.archive.model.redtag.Item;
import rosa.archive.model.redtag.Rubric;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;

/**
 *
 */
public class RedTagModelEqualsAndHashCodeTest {

    @Test
    public void blankTest() {
        EqualsVerifier
                .forClass(Blank.class)
                .usingGetClass()
                .withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void headingTest() {
        EqualsVerifier
                .forClass(Heading.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void imageTest() {
        EqualsVerifier
                .forClass(Image.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void initialTest() {
        EqualsVerifier
                .forClass(Initial.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void itemTest() {
        EqualsVerifier
                .forClass(Item.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void rubricTest() {
        EqualsVerifier
                .forClass(Rubric.class)
                .allFieldsShouldBeUsed()
                .usingGetClass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void structureColumnTest() {
        EqualsVerifier
                .forClass(StructureColumn.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void structurePageTest() {
        EqualsVerifier
                .forClass(StructurePage.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void structurePageSideTest() {
        EqualsVerifier
                .forClass(StructurePageSide.class)
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

}
