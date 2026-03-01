package emm.sys;

public class TransactionItem {
    private int id;
    private String market;
    private String custName;
    private String productName;
    private int sellingPrice;
    private int quantity;
    private int bill;
    private String paymentMode;
    private int totalBill;
    private String timestamp;
    private String transactionType; // "Paid" or "To Pay"
    private int balance; // For to_pay transactions

    // Constructor for sales (paid) transactions
    public TransactionItem(int id, String market, String custName, String productName,
                           int sellingPrice, int quantity, int bill, String paymentMode,
                           int totalBill, String timestamp) {
        this.id = id;
        this.market = market;
        this.custName = custName;
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.bill = bill;
        this.paymentMode = paymentMode;
        this.totalBill = totalBill;
        this.timestamp = timestamp;
        this.transactionType = "Paid";
        this.balance = 0;
    }

    // Constructor for to_pay transactions
    public TransactionItem(int id, String market, String custName, String productName,
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
        this.timestamp = timestamp;
        this.transactionType = "To Pay";
        this.balance = balance;
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

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
}