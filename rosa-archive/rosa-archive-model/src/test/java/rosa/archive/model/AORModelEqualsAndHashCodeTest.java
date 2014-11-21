package rosa.archive.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
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
}
