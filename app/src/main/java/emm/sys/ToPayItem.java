package emm.sys;

public class ToPayItem {
    private int id;
    private String market;
    private String custName;
    private String productName;
    private int sellingPrice;
    private int quantity;
    private int bill;
    private String paymentMode;
    private int totalBill;
    private int balance;
    private String timestamp;
    private boolean isSelected;

    public ToPayItem(int id, String market, String custName, String productName,
                     int sellingPrice, int quantity, int bill, String paymentMode,
                     int totalBill, int balance, String timestamp) {
        this.id = id;
        this.market = market;
        this.custName = custName;
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.bill = bill;
        this.paymentMode = paymentMode;
        this.totalBill = totalBill;
        this.balance = balance;
        this.timestamp = timestamp;
        this.isSelected = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getCustName() { return custName; }
    public void setCustName(String custName) { this.custName = custName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(int sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getBill() { return bill; }
    public void setBill(int bill) { this.bill = bill; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public int getTotalBill() { return totalBill; }
    public void setTotalBill(int totalBill) { this.totalBill = totalBill; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}