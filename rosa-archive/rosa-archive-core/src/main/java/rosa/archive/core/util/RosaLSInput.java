package rosa.archive.core.util;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;

public class RosaLSInput implements LSInput {

    protected String fPublicId = null;
    protected String fSystemId = null;
    protected String fBaseSystemId = null;

    protected String fData = null;

    protected String fEncoding = null;

    public RosaLSInput() {}

    public RosaLSInput(String publicId, String systemId,
                        String baseSystemId, String data,
                        String encoding) {
        fPublicId = publicId;
        fSystemId = systemId;
        fBaseSystemId = baseSystemId;
        fData = data;
        fEncoding = encoding;
    }

    @Override
    public InputStream getByteStream(){
        return null;
    }

    @Override
    public void setByteStream(InputStream byteStream){}

    @Override
    public Reader getCharacterStream(){
        return null;
    }

    @Override
    public void setCharacterStream(Reader characterStream){}

    @Override
    public String getStringData(){
        return fData;
    }

    @Override
    public void setStringData(String stringData){
        fData = stringData;
    }

    @Override
    public String getEncoding(){
        return fEncoding;
    }

    @Override
    public void setEncoding(String encoding){
        fEncoding = encoding;
    }

    @Override
    public String getPublicId(){
        return fPublicId;
    }

    @Override
    public void setPublicId(String publicId){
        fPublicId = publicId;
    }

    @Override
    public String getSystemId(){
        return fSystemId;
    }

    @Override
    public void setSystemId(String systemId){
        fSystemId = systemId;
    }

    @Override
    public String getBaseURI(){
        return fBaseSystemId;
    }

    @Override
    public void setBaseURI(String baseURI){
        fBaseSystemId = baseURI;
    }

    @Override
    public boolean getCertifiedText(){
        return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText){}
}
