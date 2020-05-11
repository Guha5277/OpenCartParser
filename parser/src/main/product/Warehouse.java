package main.product;

public class Warehouse {
    private int id;
    private String address;
    private String altName;

    public Warehouse(int id, String altName) {
        this.id = id;
        this.altName = altName;
    }

    public int getId() {
        return id;
    }

    public String getAltName() {
        return altName;
    }
}
