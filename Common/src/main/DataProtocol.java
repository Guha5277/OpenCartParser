package main;

public class DataProtocol {
    private byte[] header;
    private int dataLength;
    private String data;

    public DataProtocol(byte[] header, String data){
        this.header = header;
        this.data = data;
        dataLength = data.length();
    }

    public DataProtocol(byte[] header){
        this.header = header;
        dataLength = 0;
    }

    public byte[] getHeader() {
        return header;
    }

    public int getDataLength() {
        return dataLength;
    }

    public String getData() {
        return data;
    }
}
