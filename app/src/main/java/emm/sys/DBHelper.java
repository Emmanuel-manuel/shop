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
        super(context, "Shop.db", null, 2);
    }

    //    ............. The database name is called LoginDetails ..............
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
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // Create issue_goods table
        db.execSQL("CREATE TABLE issue_goods(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assignee TEXT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

    }
    // ............ END OF METHODS FOR CREATING NEW TABLES IN THE DB ...............

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop Table if exists users");
        db.execSQL("drop Table if exists inventory");
        db.execSQL("drop Table if exists issue_goods");
        onCreate(db);

        if (i < 2) {
            db.execSQL("CREATE TABLE inventory(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_name TEXT," +
                    "weight TEXT," +
                    "flavour TEXT," +
                    "quantity INTEGER," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
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
        if (result == -1) return false;
        else
            return true;

    }

    // New method to insert inventory data
    public boolean insertInventory(String productName, String weight, String flavour, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);

        long result = db.insert("inventory", null, values);
        return result != -1;
    }

    // New method to insert issued goods
    public boolean insertIssuedGoods(String assignee, String productName, String weight,
                                     String flavour, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("assignee", assignee);
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);

        long result = db.insert("issue_goods", null, values);
        return result != -1;
    }


    // .............. END OF METHODS THAT PUSH DATA IN THE TABLES .....................






    // ============== METHODS FOR USER SIGN-UP AND LOGIN ===============
    public Boolean checkusername(String email) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        Cursor cursor = LoginDetails.rawQuery("Select * from users where email = ?", new String[]{email});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }
//    public Boolean checkusernamepassword(String email, String password) {
//        SQLiteDatabase LoginDetails = this.getWritableDatabase();
//        Cursor cursor = LoginDetails.rawQuery("Select * from users where email = ? and password = ?", new String[]{email, password});
//        if (cursor.getCount() > 0)
//            return true;
//        else
//            return false;
//    }

    //a method to get email by role and password:
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
    // ================== END OF METHODS FOR USER SIGN-UP AND LOGIN =============================================

    // ====== METHODS FOR MANAGING INVENTORY ========================
    public Cursor getTodayInventory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT product_name, weight, flavour, quantity, timestamp " +
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
    // ================== END OF METHODS FOR MANAGING INVENTORY ================================================

    // ================== METHODS FOR MANAGING ISSUED GOODS ============================
    // Method to get all issued goods
    public Cursor getAllIssuedGoods() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM issue_goods ORDER BY timestamp DESC", null);
    }

    // Add this method to DBHelper
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
