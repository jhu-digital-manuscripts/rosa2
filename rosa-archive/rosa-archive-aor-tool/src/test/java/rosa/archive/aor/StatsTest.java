package rosa.archive.aor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatsTest {

    @Test
    public void testPageIndex() {
        assertEquals(47, new Stats("Ha2.024r").pageIndex());
        assertEquals(48, new Stats("Ha2.024v").pageIndex());
        assertEquals(7, new Stats("case_712_c27495_007").pageIndex());
        assertEquals(12, new Stats("000000012").pageIndex());

        assertEquals(-1, new Stats("FolgersHa2").pageIndex());
        assertEquals(-1, new Stats("Buchanan_MariaScotorumRegina").pageIndex());
    }

}
