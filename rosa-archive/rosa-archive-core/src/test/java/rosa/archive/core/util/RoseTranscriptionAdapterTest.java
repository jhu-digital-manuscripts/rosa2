package rosa.archive.core.util;

import java.util.function.Function;

import org.junit.Test;

public class RoseTranscriptionAdapterTest {

    @Test
    public void testSimple() {
        String xml = "<?xml version=\"1.0\"?>\n" + 
                "\n" + 
                "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.tei-c.org/ns/1.0    http://www.tei-c.org/release/xm\n" + 
                "l/tei/custom/schema/xsd/tei_ms.xsd\">\n" + 
                "<div>\n" + 
                "  <pb n=\"1r\"/>\n" + 
                "  \n" + 
                "    <figure type=\"miniature\">\n" + 
                "      <head>Frontispiece miniature, divided into quadrants. Upper left: Lover sleeping. Upper right: Lover dressing.  Lower left: Lover walking.  Lower right: Lover approaching \n" + 
                "garden.   Size: 29-line x 152 mm.  (across both text columns).1</head>\n" + 
                "    </figure>\n" + 
                "  \n" + 
                "  <cb n=\"a\"/>\n" + 
                "  <lg type=\"couplet\">\n" + 
                "    <l n=\"1\"><hi rend=\"init\">M</hi>aintes ge<expan>n</expan>s dient que en songes</l>\n" + 
                "    <milestone n=\"1\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "    <l n=\"2\">N'a se fables non et me&#xE7;onges</l>\n" + 
                "    <milestone n=\"2\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "  </lg>\n" + 
                "  <lg type=\"couplet\">\n" + 
                "    <l n=\"3\">Mais l'en puet tiex songes songier</l>\n" + 
                "    <milestone n=\"3\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "    <l n=\"4\">Qui ne sont mie men&#xE7;ongier</l>\n" + 
                "    <milestone n=\"4\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "  </lg>\n" + 
                "  <lg type=\"couplet\">\n" + 
                "    <l n=\"5\">A tuz so<expan>n</expan>t apr&#xE9;s bien apparent</l>\n" + 
                "    <milestone n=\"5\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "    <l n=\"6\">Si en puis bien trere a garant</l>\n" + 
                "    <milestone n=\"6\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "  </lg>\n" + 
                "  <lg type=\"couplet\">\n" + 
                "    <l n=\"7\">Un auctour qui ot non Macrobes</l>\n" + 
                "    <milestone n=\"7\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "    <l n=\"8\">Qui ne tint pas songes a lobes</l>\n" + 
                "    <milestone n=\"8\" ed=\"lecoy\" unit=\"line\"/>\n" + 
                "  </lg>\n" + 
                "</div></TEI>";
        
        String html = new RoseTranscriptionAdapter().toHtml(xml, Function.identity());
        
        // System.err.println(html);
    }
}
