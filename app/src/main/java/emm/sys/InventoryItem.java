package emm.sys;

public class InventoryItem {
    private String productName;
    private String weight;
    private String flavour;
    private int quantity;
    private String timestamp;

    // Constructor
    public InventoryItem(String productName, String weight, String flavour, int quantity, String timestamp) {
        this.productName = productName;
        this.weight = weight;
        this.flavour = flavour;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // Getters
    public String getProductName() { return productName; }
    public String getWeight() { return weight; }
    public String getFlavour() { return flavour; }
    public int getQuantity() { return quantity; }
    public String getTimestamp() { return timestamp; }
}
