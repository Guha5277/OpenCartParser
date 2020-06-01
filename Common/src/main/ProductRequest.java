package main;

public class ProductRequest {
    private boolean stock;
    private int regionID;
    private int storeID;
    private int strengthStart;
    private int strengthEnd;
    private int volumeStart;
    private int volumeEnd;
    private int priceStart;
    private int priceEnd;

    public ProductRequest(boolean stock, int regionID, int storeID, int strengthStart, int strengthEnd, int volumeStart, int volumeEnd, int priceStart, int priceEnd) {
        this.stock = stock;
        this.regionID = regionID;
        this.storeID = storeID;
        this.strengthStart = strengthStart;
        this.strengthEnd = strengthEnd;
        this.volumeStart = volumeStart;
        this.volumeEnd = volumeEnd;
        this.priceStart = priceStart;
        this.priceEnd = priceEnd;
    }

    public boolean isInStock() {
        return stock;
    }

    public int getRegionID() {
        return regionID;
    }

    public int getStoreID() {
        return storeID;
    }

    public int getStrengthStart() {
        return strengthStart;
    }

    public int getStrengthEnd() {
        return strengthEnd;
    }

    public int getVolumeStart() {
        return volumeStart;
    }

    public int getVolumeEnd() {
        return volumeEnd;
    }

    public int getPriceStart() {
        return priceStart;
    }

    public int getPriceEnd() {
        return priceEnd;
    }
}
