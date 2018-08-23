package rosa.archive.aor;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

public class StatsTest {

    @Test
    public void testPageIndex() {
        assertEquals(47, new Stats("Ha2.024r").pageIndex());
        assertEquals(48, new Stats("Ha2.024v").pageIndex());
        assertEquals(1, new Stats("615_d_7_f001r").pageIndex());
        assertEquals(7, new Stats("case_712_c27495_007").pageIndex());
        assertEquals(12, new Stats("000000012").pageIndex());
        assertEquals(7, new Stats("PR-R-00005-00027-F-000-00007").pageIndex());
        assertEquals(6, new Stats("WB5-12_0226_003v").pageIndex());

        assertEquals(-1, new Stats("c_120_b_4_(2)_f038bv").pageIndex());
        assertEquals(-1, new Stats("017_2").pageIndex());
        assertEquals(-1, new Stats("FolgersHa2").pageIndex());
        assertEquals(-1, new Stats("Buchanan_MariaScotorumRegina").pageIndex());
    }


    @Test
    public void testParseText() {
    	String text = "Cato repented him noth[ing] somuch, as that euer he committed any Secret to a wooman. And for that point, there is no difference betwixt a wooman, & an effeminate man.";
    	
    	String[] tokens = AoRVocabUtil.parse_text(text);
    	
    	assertEquals(2, Stream.of(tokens).filter(s -> s.equals("wooman")).count());    	
    	assertEquals(1, Stream.of(tokens).filter(s -> s.equals("nothing")).count());
    }
    
    @Test
    public void updateVocab() {
		Map<String, Integer> vocab = new HashMap<>();
		
		AoRVocabUtil.updateVocab(vocab, "moo", 3);
    	
		assertEquals(Integer.valueOf(3), vocab.get("moo"));
		
		AoRVocabUtil.updateVocab(vocab, "moo", 2);
		
		assertEquals(Integer.valueOf(5), vocab.get("moo"));
    	
    }
}
