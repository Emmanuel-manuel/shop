package emm.sys;

public class SaleItem {
    private String market;
    private String custName;
    private String product;
    private int sellingPrice;
    private int quantity;
    private int bill;

    // Constructors
    public SaleItem() {}

    public SaleItem(String market, String custName, String product, int sellingPrice, int quantity, int bill) {
        this.market = market;
        this.custName = custName;
        this.product = product;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.bill = bill;
    }

    // Getters and Setters
    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(int sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBill() {
        return bill;
    }

    public void setBill(int bill) {
        this.bill = bill;
    }

    @Override
    public String toString() {
        return "SaleItem{" +
                "market='" + market + '\'' +
                ", custName='" + custName + '\'' +
                ", product='" + product + '\'' +
                ", sellingPrice=" + sellingPrice +
                ", quantity=" + quantity +
                ", bill=" + bill +
                '}';
    }
}
