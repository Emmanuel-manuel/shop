package emm.sys;

public class IssuedGoodsItem {
    private int id;
    private String assignee;
    private String productName;
    private String weight;
    private String flavour;
    private int quantity;
    private String station;
    private String timestamp;

    public IssuedGoodsItem(int id, String assignee, String productName, String weight,
                           String flavour, int quantity, String station, String timestamp) {
        this.id = id;
        this.assignee = assignee;
        this.productName = productName;
        this.weight = weight;
        this.flavour = flavour;
        this.quantity = quantity;
        this.station = station;
        this.timestamp = timestamp;
    }

    //  ============ Getters and Setters ============
    // Getters
    public int getId() { return id; }
    public String getAssignee() { return assignee; }
    public String getProductName() { return productName; }
    public String getWeight() { return weight; }
    public String getFlavour() { return flavour; }
    public int getQuantity() { return quantity; }
    public String getStation() { return station; }
    public String getTimestamp() { return timestamp; }

    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setWeight(String weight) { this.weight = weight; }
    public void setFlavour(String flavour) { this.flavour = flavour; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setStation(String station) { this.station = station; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}