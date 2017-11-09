package rosa.archive.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.AnnotationLink;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.PhysicalLink;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TableRow;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

/**
 *
 */
public class AORModelEqualsAndHashCodeTest {

    @Test
    public void marginaliaTest() {
        EqualsVerifier
                .forClass(Marginalia.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void marginaliaLanguageTest() {
        EqualsVerifier
                .forClass(MarginaliaLanguage.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void markTest() {
        EqualsVerifier
                .forClass(Mark.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void numeralTest() {
        EqualsVerifier
                .forClass(Numeral.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void positionTest() {
        EqualsVerifier
                .forClass(Position.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void symbolTest() {
        EqualsVerifier
                .forClass(Symbol.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void underlineTest() {
        EqualsVerifier
                .forClass(Underline.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void errataTest() {
        EqualsVerifier
                .forClass(Errata.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void drawingTest() {
        EqualsVerifier
                .forClass(Drawing.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void xRefTest() {
        EqualsVerifier
                .forClass(XRef.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void annotatedPageTest() {
        EqualsVerifier
                .forClass(AnnotatedPage.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void internalRefTest() {
        EqualsVerifier
                .forClass(InternalReference.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void referenceTargetTest() {
        EqualsVerifier
                .forClass(ReferenceTarget.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void textElTest() {
        EqualsVerifier
                .forClass(TextEl.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
    }

    @Test
    public void graphTest() {
        EqualsVerifier
                .forClass(Graph.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void graphNodeTest() {
        EqualsVerifier
                .forClass(GraphNode.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void annotationLinkTest() {
        EqualsVerifier
                .forClass(AnnotationLink.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void graphTextTest() {
        EqualsVerifier
                .forClass(GraphText.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void calculationTest() {
        EqualsVerifier
                .forClass(Calculation.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void tableRowTest() {
        EqualsVerifier
                .forClass(TableRow.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void tableColTest() {
        EqualsVerifier
                .forClass(TableCell.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void graphNoteTest() {
        EqualsVerifier
                .forClass(GraphNote.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void physicalLinkTest() {
        EqualsVerifier
                .forClass(PhysicalLink.class)
                .usingGetClass()
                .allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }
}
