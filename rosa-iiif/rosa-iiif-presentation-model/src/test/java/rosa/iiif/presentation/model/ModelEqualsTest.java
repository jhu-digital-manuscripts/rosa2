package rosa.iiif.presentation.model;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;
import rosa.iiif.presentation.model.selector.SvgSelector;
import rosa.iiif.presentation.model.selector.TextQuoteSelector;

public class ModelEqualsTest {
    @Test
    public void testAnnotationList() {
        EqualsVerifier.forClass(AnnotationList.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testCanvas() {
        EqualsVerifier.forClass(Canvas.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testCollection() {
        EqualsVerifier.forClass(Collection.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testHtmlValue() {
        EqualsVerifier.forClass(HtmlValue.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testIIIFImageService() {
        EqualsVerifier.forClass(IIIFImageService.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testLayer() {
        EqualsVerifier.forClass(Layer.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testManifest() {
        EqualsVerifier.forClass(Manifest.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testPresentationRequest() {
        EqualsVerifier.forClass(PresentationRequest.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testRange() {
        EqualsVerifier.forClass(Range.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testReference() {
        EqualsVerifier.forClass(Reference.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testPresentationBase() {
        EqualsVerifier.forClass(PresentationBase.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testSequence() {
        EqualsVerifier.forClass(Sequence.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testService() {
        EqualsVerifier.forClass(Service.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testTextValue() {
        EqualsVerifier.forClass(TextValue.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testAnnotation() {
        EqualsVerifier.forClass(Annotation.class).allFieldsShouldBeUsed().withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testAnnotationSource() {
        EqualsVerifier.forClass(AnnotationSource.class).usingGetClass().allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testAnnotationTarget() {
        EqualsVerifier.forClass(AnnotationTarget.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testFragmentSelector() {
        EqualsVerifier.forClass(FragmentSelector.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }
    
    @Test
    public void testSvgSelector() {
        EqualsVerifier.forClass(SvgSelector.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testRights() {
        EqualsVerifier.forClass(Rights.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testImage() {
        EqualsVerifier.forClass(Image.class).allFieldsShouldBeUsed()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void testTextQuoteSelector() {
        EqualsVerifier.forClass(TextQuoteSelector.class).allFieldsShouldBeUsed().usingGetClass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS).verify();
    }

}
