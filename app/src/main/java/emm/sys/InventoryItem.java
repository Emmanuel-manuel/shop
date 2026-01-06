package emm.sys;

public class InventoryItem {
    private String productName;
    private String weight;
    private String flavour;
    private int quantity;
    private int balance;
    private String timestamp;

    // Constructor
    public InventoryItem(String productName, String weight, String flavour, int quantity, int balance, String timestamp) {
        this.productName = productName;
        this.weight = weight;
        this.flavour = flavour;
        this.quantity = quantity;
        this.balance = balance;
        this.timestamp = timestamp;
    }

    // Getters
    public String getProductName() { return productName; }
    public String getWeight() { return weight; }
    public String getFlavour() { return flavour; }
    public int getQuantity() { return quantity; }
    public int getBalance() { return balance; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setProductName(String productName) { this.productName = productName; }
    public void setWeight(String weight) { this.weight = weight; }
    public void setFlavour(String flavour) { this.flavour = flavour; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setBalance(int balance) { this.balance = balance; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    private boolean isLowStock;

    // Add this method
    public boolean isLowStock() {
        return balance <= 5; // Example: low stock if balance is 5 or less
    }
}
