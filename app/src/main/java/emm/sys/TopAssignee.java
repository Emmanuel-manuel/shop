package emm.sys;

public class TopAssignee {
    private int rank;
    private String name;
    private int transactions;
    private int quantity;

    public TopAssignee(int rank, String name, int transactions, int quantity) {
        this.rank = rank;
        this.name = name;
        this.transactions = transactions;
        this.quantity = quantity;
    }

    public int getRank() { return rank; }
    public String getName() { return name; }
    public int getTransactions() { return transactions; }
    public int getQuantity() { return quantity; }
}