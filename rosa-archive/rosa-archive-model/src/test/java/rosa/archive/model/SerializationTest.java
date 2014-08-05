package rosa.archive.model;

import com.google.gwt.rpc.client.ast.HasValues;
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.impl.HasValuesCommandSink;
import com.google.gwt.rpc.server.CommandServerSerializationStreamWriter;
import com.google.gwt.rpc.server.HostedModeClientOracle;
import com.google.gwt.user.client.rpc.SerializationException;
import org.junit.Test;

/**
 * Class to test the ability for model classes to be serialized
 */
public class SerializationTest {

    @Test
    public void archiveCollectionSerializationTest() throws Exception {
        checkGwtSerializability(new BookCollection());
    }

    /**
     * Check GWT serialization of input object. If an object cannot be serialized, a RuntimeException
     * is thrown.
     *
     * TODO not even sure this thing works...
     *
     * @param o
     *          The object to serialize.
     * @throws RuntimeException
     */
    private void checkGwtSerializability(Object o) throws RuntimeException {
        HostedModeClientOracle hmco = new HostedModeClientOracle();
        HasValues command = new ReturnCommand();
        HasValuesCommandSink hvcs = new HasValuesCommandSink(command);
        CommandServerSerializationStreamWriter out = new CommandServerSerializationStreamWriter(hmco, hvcs);

        try {
            out.writeObject(o);
        } catch (SerializationException e) {
            throw new RuntimeException("Object not serializable: " + o + " Caused by: " + e.getMessage(), e);
        }

        try {
            o.getClass().getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Object not serializable: " + o + " Caused by: " + e.getMessage(), e);
        }
    }

}
