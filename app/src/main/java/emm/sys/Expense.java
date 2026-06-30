package emm.sys;

public class Expense {
    private int id;
    private String category;
    private double amount;
    private String date;
    private String notes;

    public Expense() {}

    public Expense(int id, String category, double amount, String date, String notes) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}