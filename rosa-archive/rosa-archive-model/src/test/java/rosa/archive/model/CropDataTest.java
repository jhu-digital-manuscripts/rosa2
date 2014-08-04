package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @see rosa.archive.model.CropData
 */
public class CropDataTest {
    private double[] stuff = { 1.1, 2.2, 3.3, 4.4 };

    private CropData data;

    @Before
    public void setup() {
        this.data = new CropData();

        data.setId("ThisIsAnId");
        data.setLeft(stuff[0]);
        data.setRight(stuff[1]);
        data.setTop(stuff[2]);
        data.setBottom(stuff[3]);
    }

    @Test
    public void asArrayTest() {
        double[] fromData = data.asArray();

        assertNotNull(fromData);
        assertEquals(4, fromData.length);

        double delta = 0.0000001;
        for (int i = 0; i < 4; i++) {
            assertEquals(stuff[i], fromData[i], delta);
        }
    }
}
