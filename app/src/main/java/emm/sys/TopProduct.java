package emm.sys;

public class TopProduct {
    private int rank;
    private String name;
    private int quantity;
    private double revenue;

    public TopProduct(int rank, String name, int quantity, double revenue) {
        this.rank = rank;
        this.name = name;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public int getRank() { return rank; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getRevenue() { return revenue; }
}