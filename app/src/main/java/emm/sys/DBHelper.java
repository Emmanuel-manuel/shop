package emm.sys;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Shop.db";

    public DBHelper(Context context) {

        super(context, "Shop.db", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    // ......... METHODS FOR CREATING NEW TABLES IN THE DB ...............
        // Create users table
        db.execSQL("create Table users(role TEXT, email TEXT primary key, password TEXT)");

        // Create inventory table
        db.execSQL("CREATE TABLE inventory(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "balance INTEGER,"+
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // Create issue_goods table
        db.execSQL("CREATE TABLE issue_goods(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assignee TEXT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "station TEXT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

    }
    // ............ END OF METHODS FOR CREATING NEW TABLES IN THE DB ...............

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Add balance column to inventory table if it doesn't exist
            db.execSQL("ALTER TABLE inventory ADD COLUMN balance INTEGER DEFAULT 0");

            // Update existing records to set balance = quantity
            db.execSQL("UPDATE inventory SET balance = quantity WHERE balance IS NULL OR balance = 0");
        }
        if (oldVersion < 5) {
            // Add station column to issue_goods table
            db.execSQL("ALTER TABLE issue_goods ADD COLUMN station TEXT DEFAULT ''");
        }
    }

    // .............. METHODS THAT PUSH DATA IN THE TABLES .....................
    // Method to insert User's data
    public Boolean insertData(String role, String email, String password) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("role", role);
        contentValues.put("email", email);
        contentValues.put("password", password);
        long result = LoginDetails.insert("users", null, contentValues);
        return result != -1;
    }

    // Updated method to insert inventory data - sets balance = quantity
    public boolean insertInventory(String productName, String weight, String flavour, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);
        values.put("balance", quantity); // Set balance equal to quantity

        long result = db.insert("inventory", null, values);
        return result != -1;
    }

    // Method to get current balance for a product
    public int getProductBalance(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int balance = 0;

        Cursor cursor = db.rawQuery(
                "SELECT balance FROM inventory WHERE product_name = ? ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName}
        );

        if (cursor.moveToFirst()) {
            balance = cursor.getInt(0);
        }
        cursor.close();

        return balance;
    }




    // Method to update inventory balance with specific new balance value
    public boolean updateInventoryBalance(String productName, int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Update the most recent inventory record for this product
        ContentValues values = new ContentValues();
        values.put("balance", newBalance);

        int rowsAffected = db.update("inventory", values,
                "product_name = ? AND id = (SELECT id FROM inventory WHERE product_name = ? ORDER BY timestamp DESC LIMIT 1)",
                new String[]{productName, productName});

        return rowsAffected > 0;
    }

    // Updated method to insert issued goods and update inventory balance
    public boolean insertIssuedGoods(String assignee, String productName, String weight,
                                     String flavour, int quantity, String station, int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Start transaction
        db.beginTransaction();
        try {
            // Insert into issue_goods table
            ContentValues values = new ContentValues();
            values.put("assignee", assignee);
            values.put("product_name", productName);
            values.put("weight", weight);
            values.put("flavour", flavour);
            values.put("quantity", quantity);
            values.put("station", station);

            long result = db.insert("issue_goods", null, values);

            if (result != -1) {
                // Update inventory balance with the new balance value
                boolean updateSuccess = updateInventoryBalance(productName, newBalance);
                if (updateSuccess) {
                    db.setTransactionSuccessful();
                    return true;
                }
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }




    // ============== METHODS FOR USER SIGN-UP AND LOGIN ===============
    public Boolean checkusername(String email) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        Cursor cursor = LoginDetails.rawQuery("Select * from users where email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Method to get email by role and password
    public String getEmailByRolePassword(String role, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String email = null;

        Cursor cursor = db.rawQuery(
                "SELECT email FROM users WHERE role = ? AND password = ? LIMIT 1",
                new String[]{role, password}
        );

        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }
        cursor.close();
        return email;
    }

    // ====== METHODS FOR MANAGING INVENTORY ========================
    public Cursor getTodayInventory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT product_name, weight, flavour, quantity, balance, timestamp " +
                "FROM inventory WHERE date(timestamp) = date('now') " +
                "ORDER BY timestamp DESC", null);
    }

    public boolean checkInventoryExists(String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM inventory WHERE " +
                "product_name = ? AND " +
                "weight = ? AND " +
                "flavour = ? AND " +
                "date(timestamp) = date('now')";

        Cursor cursor = db.rawQuery(query, new String[]{productName, weight, flavour});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ================== METHODS FOR MANAGING ISSUED GOODS ============================
    public Cursor getAllIssuedGoods() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM issue_goods ORDER BY timestamp DESC", null);
    }

    public boolean checkDuplicateIssue(String assignee, String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM issue_goods WHERE " +
                "assignee = ? AND " +
                "product_name = ? AND " +
                "weight = ? AND " +
                "flavour = ? AND " +
                "date(timestamp) = date('now')";

        Cursor cursor = db.rawQuery(query, new String[]{assignee, productName, weight, flavour});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
