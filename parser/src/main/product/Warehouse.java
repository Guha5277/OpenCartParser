package main.product;

public class Warehouse {
    private int id;
    private String altName;
    private int region;
    private String city;
    private String address;
    private int remains;

    public Warehouse(int id, String altName, int region, String city, String address) {
        this.id = id;
        this.altName = altName;
        this.region = region;
        this.city = city;
        this.address = address;
    }

    public Warehouse(int id, String altName, int remains) {
        this.id = id;
        this.altName = altName;
        this.remains = remains;
    }

    public int getId() {
        return id;
    }
    public String getAltName() {
        return altName;
    }
    public int getRegion() {
        return region;
    }
    public String getCity() {
        return city;
    }
    public String getAddress() {
        return address;
    }
}
