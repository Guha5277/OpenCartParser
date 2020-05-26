package main.product;

public class Warehouse {
    private int id;
    private String altName;
    private int region;

    public Warehouse(int id, String altName) {
        this.id = id;
        this.altName = altName;
    }

    public Warehouse(int id, String altName, int region) {
        this(id, altName);
        this.region = region;
    }

    public int getId() {
        return id;
    }

    public String getAltName() {
        return altName;
    }
}
