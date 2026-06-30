package emm.sys;

public class EmployeeReceivedGoodsItem {
    private int id;
    private String productName;
    private String weight;
    private String flavour;
    private int quantity;
    private String station;
    private String timestamp;

    public EmployeeReceivedGoodsItem(int id, String productName, String weight,
                                     String flavour, int quantity, String station, String timestamp) {
        this.id = id;
        this.productName = productName;
        this.weight = weight;
        this.flavour = flavour;
        this.quantity = quantity;
        this.station = station;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getProductName() { return productName; }
    public String getWeight() { return weight; }
    public String getFlavour() { return flavour; }
    public int getQuantity() { return quantity; }
    public String getStation() { return station; }
    public String getTimestamp() { return timestamp; }
}