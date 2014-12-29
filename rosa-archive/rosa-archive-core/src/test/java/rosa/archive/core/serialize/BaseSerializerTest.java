package rosa.archive.core.serialize;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rosa.archive.core.config.AppConfig;

/**
 *
 */
public abstract class BaseSerializerTest {

    protected List<String> errors;
    @Mock
    protected AppConfig config;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(config.getCHARSET()).thenReturn("UTF-8");
        when(config.languages()).thenReturn(new String[] {"en", "fr"});

        when(config.getMetadataCommonNameTag()).thenReturn("commonName");
        when(config.getMetadataCurrentLocationTag()).thenReturn("settlement");
        when(config.getMetadataDateTag()).thenReturn("date");
        when(config.getMetadataHeightTag()).thenReturn("height");
        when(config.getMetadataWidthTag()).thenReturn("width");
        when(config.getMetadataMaterialTag()).thenReturn("material");
        when(config.getMetadataMeasureTag()).thenReturn("measure");
        when(config.getMetadataNumIllustrationsTag()).thenReturn("illustrations");
        when(config.getMetadataNumPagesTag()).thenReturn("quantity");
        when(config.getMetadataShelfmarkTag()).thenReturn("idno");
        when(config.getMetadataOriginTag()).thenReturn("pubPlace");
        when(config.getMetadataRepositoryTag()).thenReturn("repository");
        when(config.getMetadataTypeTag()).thenReturn("format");
        when(config.getMetadataYearStartTag()).thenReturn("notBefore");
        when(config.getMetadataYearEndTag()).thenReturn("notAfter");
        when(config.getMetadataTextsTag()).thenReturn("msItem");
        when(config.getMetadataTextsLinesPerColTag()).thenReturn("linesPerColumn");
        when(config.getMetadataTextsColsPerPageTag()).thenReturn("columnsPerPage");
        when(config.getMetadataTextsLeavesPerGatheringTag()).thenReturn("leavesPerGathering");
        when(config.getMetadataTextsNumPagesTag()).thenReturn("folios");
        when(config.getMetadataTextsIdTag()).thenReturn("textid");
        when(config.getMetadataTextsTitleTag()).thenReturn("title");
        when(config.getMetadataTextsLocusTag()).thenReturn("locus");
        when(config.getMetadataTextsFirstPageTag()).thenReturn("from");
        when(config.getMetadataTextsLastPageTag()).thenReturn("to");

        errors = new ArrayList<>();
    }

    @Test
    public abstract void readTest() throws IOException;

    @Test
    public abstract void writeTest() throws IOException;

}
