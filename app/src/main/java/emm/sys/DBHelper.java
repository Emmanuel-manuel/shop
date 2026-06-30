package emm.sys;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

//import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Shop.db";

    public DBHelper(Context context) {

        super(context, "Shop.db", null, 11);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    // ......... METHODS FOR CREATING NEW TABLES IN THE DB ...............
        // Create users table
        db.execSQL("create Table users(role TEXT, email TEXT primary key, password TEXT)");

        // Create product_details table
        db.execSQL("CREATE TABLE product_details(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "buying_price INTEGER," +
                "selling_price INTEGER," +
                "profit INTEGER," + // Adds profit column
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");

        // Create inventory table
        db.execSQL("CREATE TABLE inventory(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "balance INTEGER,"+
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");

        // Create issue_goods table
        db.execSQL("CREATE TABLE issue_goods(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assignee TEXT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "station TEXT," +
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");


        // Create sales table
        db.execSQL("CREATE TABLE sales(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "market TEXT," +
                "cust_name TEXT," +
                "product_name TEXT," +
                "selling_price INTEGER," +
                "quantity INTEGER," +
                "bill INTEGER," +
                "payment_mode TEXT," +
                "total_bill INTEGER," +
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");

        // Create to_pay table
        db.execSQL("CREATE TABLE to_pay(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "market TEXT," +
                "cust_name TEXT," +
                "product_name TEXT," +
                "selling_price INTEGER," +
                "quantity INTEGER," +
                "bill INTEGER," +
                "payment_mode TEXT," +
                "total_bill INTEGER," +
                "balance INTEGER," +
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");

        // Create notes table
        db.execSQL("CREATE TABLE notes(" +
                           "id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                           "title     TEXT," +
                           "content   TEXT," +
                           "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");  // Changed to localtime

        // Create emp_received_goods table
        db.execSQL("CREATE TABLE emp_received_goods(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "assignee TEXT," +
                "product_name TEXT," +
                "weight TEXT," +
                "flavour TEXT," +
                "quantity INTEGER," +
                "station TEXT," +
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");


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
        }if (oldVersion < 6) {
            // Create product_details table if upgrading from older version
            db.execSQL("CREATE TABLE IF NOT EXISTS product_details(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_name TEXT," +
                    "weight TEXT," +
                    "flavour TEXT," +
                    "price INTEGER," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
        if (oldVersion < 7) {
            // Rename old price column to buying_price and add selling_price column
            db.execSQL("ALTER TABLE product_details RENAME TO product_details_old");

            db.execSQL("CREATE TABLE product_details(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "product_name TEXT," +
                    "weight TEXT," +
                    "flavour TEXT," +
                    "buying_price INTEGER," +
                    "selling_price INTEGER," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // Copy data from old table, set selling_price = buying_price for existing records
            db.execSQL("INSERT INTO product_details (id, product_name, weight, flavour, buying_price, selling_price, timestamp) " +
                    "SELECT id, product_name, weight, flavour, price, price, timestamp FROM product_details_old");

            db.execSQL("DROP TABLE product_details_old");
        }

        if (oldVersion < 8) { // Increment version to 8
            // Add profit column to product_details table
            db.execSQL("ALTER TABLE product_details ADD COLUMN profit INTEGER DEFAULT 0");

            // Update existing records to calculate profit
            db.execSQL("UPDATE product_details SET profit = selling_price - buying_price WHERE profit = 0");
        }

        if (oldVersion < 9) {
            // Create sales table
            db.execSQL("CREATE TABLE IF NOT EXISTS sales(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "market TEXT," +
                    "cust_name TEXT," +
                    "product_name TEXT," +
                    "selling_price INTEGER," +
                    "quantity INTEGER," +
                    "bill INTEGER," +
                    "payment_mode TEXT," +
                    "total_bill INTEGER," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // Create to_pay table
            db.execSQL("CREATE TABLE IF NOT EXISTS to_pay(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "market TEXT," +
                    "cust_name TEXT," +
                    "product_name TEXT," +
                    "selling_price INTEGER," +
                    "quantity INTEGER," +
                    "bill INTEGER," +
                    "payment_mode TEXT," +
                    "total_bill INTEGER," +
                    "balance INTEGER," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }

        if (oldVersion < 10) {
                   db.execSQL("CREATE TABLE IF NOT EXISTS notes(" +
                           "id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                           "title     TEXT," +
                           "content   TEXT," +
                           "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");  // Changed to localtime
        }

        if (oldVersion < 11) {
            // Create emp_received_goods table
            db.execSQL("CREATE TABLE IF NOT EXISTS emp_received_goods(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "assignee TEXT," +
                    "product_name TEXT," +
                    "weight TEXT," +
                    "flavour TEXT," +
                    "quantity INTEGER," +
                    "station TEXT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }

    }

    // .............. GENESIS OF METHODS THAT PUSH DATA IN THE TABLES .....................

    // +++++++++++++ USERS RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ============ Method to insert User's data ============
    public Boolean insertData(String role, String email, String password) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("role", role);
        contentValues.put("email", email);
        contentValues.put("password", password);
        long result = LoginDetails.insert("users", null, contentValues);
        return result != -1;
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


    // ++++++++++++++++++++  END OF USERS RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++++++




    // ++++++++++++++++++++ GENESIS OF PRODUCT RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ============ Method to insert product details ============
    public boolean insertProductDetails(String productName, String weight, String flavour,
                                        int buyingPrice, int sellingPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("buying_price", buyingPrice);
        values.put("selling_price", sellingPrice);
        values.put("profit", sellingPrice - buyingPrice); // Calculates and store profit

        long result = db.insert("product_details", null, values);
        return result != -1;
    }

    // ============ updateProductPrices method ============
    public boolean updateProductPrices(String productName, String weight, String flavour,
                                       int newBuyingPrice, int newSellingPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("buying_price", newBuyingPrice);
        values.put("selling_price", newSellingPrice);
        values.put("profit", newSellingPrice - newBuyingPrice); // Update profit

        int rowsAffected = db.update("product_details", values,
                "product_name = ? AND weight = ? AND flavour = ?",
                new String[]{productName, weight, flavour});
        return rowsAffected > 0;
    }

    // ============ NEW METHOD: Check if product details already exist ============
    public boolean checkProductDetailsExists(String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM product_details WHERE " +
                "product_name = ? AND " +
                "weight = ? AND " +
                "flavour = ?";

        Cursor cursor = db.rawQuery(query, new String[]{productName, weight, flavour});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ============ NEW METHOD: Get all product details ============
    public Cursor getAllProductDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM product_details ORDER BY product_name", null);
    }

    // ============ NEW METHOD: Get product by name ============
    public Cursor getProductByName(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM product_details WHERE product_name = ?",
                new String[]{productName});
    }

    // ============ NEW METHOD: Update product price ============
    public boolean updateProductPrice(String productName, String weight, String flavour, int newSellingPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("selling_price", newSellingPrice);

        // Get current buying price to calculate new profit
        Cursor cursor = db.rawQuery(
                "SELECT buying_price FROM product_details WHERE product_name = ? AND weight = ? AND flavour = ?",
                new String[]{productName, weight, flavour}
        );

        if (cursor.moveToFirst()) {
            int buyingPrice = cursor.getInt(0);
            values.put("profit", newSellingPrice - buyingPrice); // Calculates new profit
        }
        cursor.close();

        int rowsAffected = db.update("product_details", values,
                "product_name = ? AND weight = ? AND flavour = ?",
                new String[]{productName, weight, flavour});
        return rowsAffected > 0;
    }


    // ============ Method to get all product names from product_details table ============
    public List<String> getAllProductNamesFromProductDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> productNames = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT product_name FROM product_details ORDER BY product_name",
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    productNames.add(name);
                }
            }
            cursor.close();
        }

        return productNames;
    }

    // ============ Method to get product details (weight, flavour) from product_details ============
    public Cursor getProductDetailsFromProductDetails(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT weight, flavour FROM product_details WHERE product_name = ? LIMIT 1",
                new String[]{productName}
        );
    }


    // Optional: Added this method in-case programmer wants to check product existence at any time
    public boolean productExistsAnyTime(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM inventory WHERE product_name = ? LIMIT 1",
                new String[]{productName}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    // ============ Method to delete a product ============
    public boolean deleteProduct(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete("product_details", "id = ?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    // ============ Method to update all product fields ============
    public boolean updateProductDetails(int id, String productName, String weight,
                                        String flavour, int buyingPrice, int sellingPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("buying_price", buyingPrice);
        values.put("selling_price", sellingPrice);
        values.put("profit", sellingPrice - buyingPrice); // Calculates profit

        int rowsAffected = db.update("product_details", values, "id = ?",
                new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    // ============ Method to delete all products ============
    public boolean deleteAllProducts() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete("product_details", null, null);
        return rowsAffected > 0;
    }

    // ============ Method to get all unique product names from product_details table ============
    public List<String> getAllProductNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> productNames = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT product_name FROM product_details ORDER BY product_name",
                null
        );

        while (cursor.moveToNext()) {
            productNames.add(cursor.getString(0));
        }
        cursor.close();

        return productNames;
    }



    // +++++++++++++++++++++ END OF PRODUCT RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++




    // +++++++++++++++++++++ GENESIS OF INVENTORY RELATED METHODS ++++++++++++++++++++++++++++++++++++++++++++++++
    // ============ Updated method to insert inventory data - sets balance = quantity ============
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


    // ============  Method to get current balance for a product ============
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

    // ============ Method to update inventory balance with specific new balance value =====
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

    public boolean updateSpecificInventoryBalance(String productName, String weight, String flavour, int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        // Update the specific product variant for today
        ContentValues values = new ContentValues();
        values.put("balance", newBalance);

        int rowsAffected = db.update("inventory", values,
                "product_name = ? AND weight = ? AND flavour = ? AND date(timestamp) = ?",
                new String[]{productName, weight, flavour, todayDate});

        return rowsAffected > 0;
    }

    // to delete the top if the bottom works just fine


    // ============ Method to get current balance for specific product variant ============
    public int getSpecificProductBalance(String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();
        int balance = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT balance FROM inventory WHERE product_name = ? AND weight = ? AND flavour = ? AND date(timestamp) = ? ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName, weight, flavour, todayDate}
        );

        if (cursor.moveToFirst()) {
            balance = cursor.getInt(0);
        }
        cursor.close();

        return balance;
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

    // ============ Method to get today's balance for a specific product ============
    public int getTodayProductBalance(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int balance = 0;

        Cursor cursor = db.rawQuery(
                "SELECT balance FROM inventory WHERE product_name = ? AND date(timestamp) = date('now') ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName}
        );

        if (cursor.moveToFirst()) {
            balance = cursor.getInt(0);
        }
        cursor.close();

        return balance;
    }

    // ============ Method to check if product exists in today's inventory ============
    public boolean isProductReceivedToday(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM inventory WHERE product_name = ? AND date(timestamp) = date('now') LIMIT 1",
                new String[]{productName}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }
    // ============ Method to get weight for a product from today's inventory ============
    public String getProductWeight(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String weight = null;

        Cursor cursor = db.rawQuery(
                "SELECT weight FROM inventory WHERE product_name = ? AND date(timestamp) = date('now') LIMIT 1",
                new String[]{productName}
        );

        if (cursor.moveToFirst()) {
            weight = cursor.getString(0);
        }
        cursor.close();

        return weight;
    }

    // ============ Method to check if product exists in database (today) ============
    public boolean productExists(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM inventory WHERE product_name = ? AND date(timestamp) = date('now') LIMIT 1",
                new String[]{productName}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    // ============ Method to get all unique weights for a product ============
    public List<String> getAllWeightsForProduct(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> weights = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT weight FROM inventory WHERE product_name = ? AND date(timestamp) = date('now') ORDER BY weight",
                new String[]{productName}
        );

        while (cursor.moveToNext()) {
            weights.add(cursor.getString(0));
        }
        cursor.close();

        return weights;
    }
    // ============ Method to get all products with available balance from inventory for TODAY============
    public List<String> getProductsWithAvailableBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> products = new ArrayList<>();

        // Get today's date in device's local timezone (YYYY-MM-DD format)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        // Get distinct product names where balance > 0 for TODAY
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT product_name FROM inventory WHERE date(timestamp) = ? AND balance > 0 ORDER BY product_name",
                new String[]{todayDate}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String productName = cursor.getString(0);
                if (productName != null && !productName.trim().isEmpty()) {
                    products.add(productName);
                }
            }
            cursor.close();
        }

        return products;
    }

    // ============ Method to get weight and flavour for a product from TODAY'S inventory table (device local date) ============
    public Cursor getInventoryProductDetails(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date in device's local timezone (YYYY-MM-DD format)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        // Get the latest weight, flavour, and balance for TODAY
        return db.rawQuery(
                "SELECT weight, flavour, balance FROM inventory " +
                        "WHERE product_name = ? AND date(timestamp) = ? AND balance > 0 " +
                        "ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName, todayDate}
        );
    }

    // ============ Method to get product details from ANY inventory (not limited to today) ============
    public Cursor getAnyInventoryProductDetails(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT weight, flavour, balance FROM inventory WHERE product_name = ? AND balance > 0 ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName}
        );
    }

    // ============ Method to get inventory summary by date ============
    public Cursor getInventorySummaryByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT product_name, SUM(quantity) as total_quantity, SUM(balance) as total_balance " +
                        "FROM inventory WHERE date(timestamp) = ? " +
                        "GROUP BY product_name " +
                        "ORDER BY total_quantity DESC",
                new String[]{date}
        );
    }

    // ============ Method to get all dates that have inventory data ============
    public Set<String> getDatesWithInventoryData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Set<String> dates = new HashSet<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT date(timestamp) as date FROM inventory ORDER BY date DESC",
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(0);
                dates.add(date);
            }
            cursor.close();
        }

        return dates;
    }


    // ++++++++++++++++++ END OF INVENTORY RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++++++





    // ++++++++++++++++++ GENESIS OF ISSUED GOODS METHODS ++++++++++++++++++++++++++++++++++++++++++++++++
    // ========= Updated method to insert issued goods and update inventory balance ======
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
                // FIXED: Update inventory balance for the specific product variant
                boolean updateSuccess = updateSpecificInventoryBalance(productName, weight, flavour, newBalance);
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


    // ================== METHODS FOR MANAGING ISSUED GOODS ============================
    public Cursor getAllIssuedGoods() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM issue_goods ORDER BY timestamp DESC", null);
    }

    // ============ METHOD FOR CHECKING DUPLICATE ENTRIES WHILE ISSUING GOODS ============
    public boolean checkDuplicateIssue(String assignee, String productName, String weight, String flavour, String station) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM issue_goods WHERE " +
                "assignee = ? AND " +
                "product_name = ? AND " +
                "weight = ? AND " +
                "flavour = ? AND " +
                "station = ? AND " +
                "date(timestamp) = date('now')";

        Cursor cursor = db.rawQuery(query, new String[]{assignee, productName, weight, flavour, station});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ============  Method to get issued goods by ID ============
    public Cursor getIssuedGoodsById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM issue_goods WHERE id = ?", new String[]{String.valueOf(id)});
    }

    //  ============ Method to update issued goods ============
    public boolean updateIssuedGoods(int id, String assignee, String productName, String weight,
                                     String flavour, int quantity, String station) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("assignee", assignee);
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);
        values.put("station", station);

        int rowsAffected = db.update("issue_goods", values, "id = ?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    // ============ Method to delete issued goods and restore inventory balance ============
    public boolean deleteIssuedGoods(int id, String productName, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            // First, restore the inventory balance
            int currentBalance = getProductBalance(productName);
            int newBalance = currentBalance + quantity;
            boolean balanceUpdated = updateInventoryBalance(productName, newBalance);

            if (balanceUpdated) {
                // Then delete the issued goods record
                int rowsDeleted = db.delete("issue_goods", "id = ?", new String[]{String.valueOf(id)});
                if (rowsDeleted > 0) {
                    db.setTransactionSuccessful();
                    return true;
                }
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    //  ============ Method to update issued goods and adjust inventory balance ============
    public boolean updateIssuedGoodsWithBalance(int id, String assignee, String productName, String weight,
                                                String flavour, int oldQuantity, int newQuantity,
                                                String station, int currentBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            // Calculate the balance difference
            int quantityDifference = oldQuantity - newQuantity;
            int newBalance = currentBalance + quantityDifference;

            // Update inventory balance
            boolean balanceUpdated = updateInventoryBalance(productName, newBalance);

            if (balanceUpdated) {
                // Update issued goods record
                ContentValues values = new ContentValues();
                values.put("assignee", assignee);
                values.put("product_name", productName);
                values.put("weight", weight);
                values.put("flavour", flavour);
                values.put("quantity", newQuantity);
                values.put("station", station);

                int rowsAffected = db.update("issue_goods", values, "id = ?", new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    db.setTransactionSuccessful();
                    return true;
                }
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }
    // ============ Method to update issued goods with the new balance calculation logic ============
    public boolean updateIssuedGoodsWithNewLogic(int id, String assignee, String productName, String weight,
                                                 String flavour, int oldQuantity, int newQuantity,
                                                 String station, int newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            // FIXED: Update inventory balance for the specific product variant
            boolean balanceUpdated = updateSpecificInventoryBalance(productName, weight, flavour, newBalance);

            if (balanceUpdated) {
                // Update issued goods record
                ContentValues values = new ContentValues();
                values.put("assignee", assignee);
                values.put("product_name", productName);
                values.put("weight", weight);
                values.put("flavour", flavour);
                values.put("quantity", newQuantity);
                values.put("station", station);

                int rowsAffected = db.update("issue_goods", values, "id = ?", new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    db.setTransactionSuccessful();
                    return true;
                }
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ============ Method to get balance for specific product+weight+flavour combination ============
    public Cursor getSpecificInventoryDetails(String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date in device's local timezone (YYYY-MM-DD format)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        return db.rawQuery(
                "SELECT weight, flavour, balance FROM inventory " +
                        "WHERE product_name = ? AND weight = ? AND flavour = ? AND date(timestamp) = ? " +
                        "ORDER BY timestamp DESC LIMIT 1",
                new String[]{productName, weight, flavour, todayDate}
        );
    }

    // ============ Method to check if specific product variant exists today ============
    public boolean checkSpecificProductExists(String productName, String weight, String flavour) {
        SQLiteDatabase db = this.getReadableDatabase();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT * FROM inventory WHERE product_name = ? AND weight = ? AND flavour = ? AND date(timestamp) = ? LIMIT 1",
                new String[]{productName, weight, flavour, todayDate}
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


    //    =============== Method for date filtering in ViewIssuedGoodsFragment ===========
    public Cursor getIssuedGoodsByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM issue_goods WHERE date(timestamp) = ? ORDER BY timestamp DESC",
                new String[]{date});
    }

    // ============ Method to get issued goods summary by date ============
    public Cursor getIssuedGoodsSummaryByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT product_name, SUM(quantity) as total_quantity " +
                        "FROM issue_goods WHERE date(timestamp) = ? " +
                        "GROUP BY product_name " +
                        "ORDER BY total_quantity DESC",
                new String[]{date}
        );
    }

    // ============ Method to get all dates that have issued goods data ============
    public Set<String> getDatesWithIssuedData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Set<String> dates = new HashSet<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT date(timestamp) as date FROM issue_goods ORDER BY date DESC",
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(0);
                dates.add(date);
            }
            cursor.close();
        }

        return dates;
    }


    // ++++++++++++++++ END OF ISSUED GOODS RELATED METHODS ++++++++++++++++++++++++++++++++++++++++++++




// ============ GENESIS OF EMPLOYEE RECEIVED GOODS METHODS ============

    // Insert employee received goods
    public boolean insertEmployeeReceivedGoods(String assignee, String productName, String weight,
                                               String flavour, int quantity, String station) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("assignee", assignee);
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);
        values.put("station", station);

        long result = db.insert("emp_received_goods", null, values);
        return result != -1;
    }

    // Get all employee received goods
    public Cursor getAllEmployeeReceivedGoods() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM emp_received_goods ORDER BY timestamp DESC", null);
    }

    // Get employee received goods by date
    public Cursor getEmployeeReceivedGoodsByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM emp_received_goods WHERE date(timestamp) = ? ORDER BY timestamp DESC",
                new String[]{date});
    }

    // Get employee received goods by assignee
    public Cursor getEmployeeReceivedGoodsByAssignee(String assignee) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM emp_received_goods WHERE assignee = ? ORDER BY timestamp DESC",
                new String[]{assignee});
    }

    // Get employee received goods by assignee and date
    public Cursor getEmployeeReceivedGoodsByAssigneeAndDate(String assignee, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM emp_received_goods WHERE assignee = ? AND date(timestamp) = ? ORDER BY timestamp DESC",
                new String[]{assignee, date});
    }

    // Delete employee received goods
    public boolean deleteEmployeeReceivedGoods(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete("emp_received_goods", "id = ?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    // Update employee received goods
    public boolean updateEmployeeReceivedGoods(int id, String assignee, String productName, String weight,
                                               String flavour, int quantity, String station) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("assignee", assignee);
        values.put("product_name", productName);
        values.put("weight", weight);
        values.put("flavour", flavour);
        values.put("quantity", quantity);
        values.put("station", station);

        int rowsAffected = db.update("emp_received_goods", values, "id = ?", new String[]{String.valueOf(id)});
        return rowsAffected > 0;
    }

    // Get total quantity received by employee today
    public int getTodayEmployeeReceivedTotal(String assignee) {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM emp_received_goods WHERE assignee = ? AND date(timestamp) = ?",
                new String[]{assignee, todayDate}
        );

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    // Get all unique assignees (employees) who have received goods
    public List<String> getAllEmployeeAssignees() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> assignees = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT assignee FROM emp_received_goods ORDER BY assignee",
                null
        );

        while (cursor.moveToNext()) {
            assignees.add(cursor.getString(0));
        }
        cursor.close();

        return assignees;
    }


    // ============ END OF EMPLOYEE RECEIVED GOODS METHODS =====================







    // ============ Method to get daily totals ============
    public Map<String, Integer> getDailyTotals(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Integer> totals = new HashMap<>();

        // Get total inventory
        Cursor inventoryCursor = db.rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) as total_inventory FROM inventory WHERE date(timestamp) = ?",
                new String[]{date}
        );
        if (inventoryCursor.moveToFirst()) {
            totals.put("inventory", inventoryCursor.getInt(0));
        }
        inventoryCursor.close();

        // Get total issued
        Cursor issuedCursor = db.rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) as total_issued FROM issue_goods WHERE date(timestamp) = ?",
                new String[]{date}
        );
        if (issuedCursor.moveToFirst()) {
            totals.put("issued", issuedCursor.getInt(0));
        }
        issuedCursor.close();

        return totals;
    }

    // ============ Method to get all dates with any data ============
    public Set<String> getAllDatesWithData() {
        Set<String> dates = new HashSet<>();
        dates.addAll(getDatesWithInventoryData());
        dates.addAll(getDatesWithIssuedData());
        return dates;
    }

    // ============ Method to extract day numbers from dates ============
    public Set<Integer> getHighlightedDaysForMonth(int year, int month) {
        Set<Integer> highlightedDays = new HashSet<>();

        // Format month to have leading zero if needed
        String monthStr = String.format("%02d", month + 1); // Month is 0-indexed in Calendar
        String yearStr = String.valueOf(year);

        SQLiteDatabase db = this.getReadableDatabase();

        // Query for dates in the specified month and year
        String query = "SELECT DISTINCT date(timestamp) as date " +
                "FROM (SELECT timestamp FROM inventory UNION SELECT timestamp FROM issue_goods) " +
                "WHERE strftime('%Y', date) = ? AND strftime('%m', date) = ?";

        Cursor cursor = db.rawQuery(query, new String[]{yearStr, monthStr});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(0);
                if (date != null && date.length() >= 10) {
                    // Extract day from date string (format: YYYY-MM-DD)
                    String dayStr = date.substring(8, 10);
                    try {
                        int day = Integer.parseInt(dayStr);
                        highlightedDays.add(day);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            cursor.close();
        }

        return highlightedDays;
    }


// ++++++++++++++++++++ GENESIS OF SALES RELATED METHODS +++++++++++++++++++++++++++++++++++++++++++++++++++++

    // Sales related methods
    public boolean insertSale(String market, String custName, String productName,
                              int sellingPrice, int quantity, int bill, String paymentMode, int totalBill) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("market", market);
        values.put("cust_name", custName);
        values.put("product_name", productName);
        values.put("selling_price", sellingPrice);
        values.put("quantity", quantity);
        values.put("bill", bill);
        values.put("payment_mode", paymentMode);
        values.put("total_bill", totalBill);

        long result = db.insert("sales", null, values);
        return result != -1;
    }

    public boolean insertToPay(String market, String custName, String productName,
                               int sellingPrice, int quantity, int bill, String paymentMode,
                               int totalBill, int balance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("market", market);
        values.put("cust_name", custName);
        values.put("product_name", productName);
        values.put("selling_price", sellingPrice);
        values.put("quantity", quantity);
        values.put("bill", bill);
        values.put("payment_mode", paymentMode);
        values.put("total_bill", totalBill);
        values.put("balance", balance);

        long result = db.insert("to_pay", null, values);
        return result != -1;
    }

    public List<String> getDistinctMarkets() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> markets = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT market FROM sales WHERE market IS NOT NULL AND market != '' ORDER BY market", null);

        while (cursor.moveToNext()) {
            markets.add(cursor.getString(0));
        }
        cursor.close();
        return markets;
    }

    public List<String> getDistinctCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> customers = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT DISTINCT cust_name FROM sales WHERE cust_name IS NOT NULL AND cust_name != '' ORDER BY cust_name", null);

        while (cursor.moveToNext()) {
            customers.add(cursor.getString(0));
        }
        cursor.close();
        return customers;
    }

    public int getProductSellingPrice(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int price = 0;

        Cursor cursor = db.rawQuery("SELECT selling_price FROM product_details WHERE product_name = ? LIMIT 1",
                new String[]{productName});

        if (cursor.moveToFirst()) {
            price = cursor.getInt(0);
        }
        cursor.close();
        return price;
    }
// =========== ENHANCED SALES ANALYTICS METHODS ===========

    // Get today's total sales revenue
    public int getTodayTotalRevenue() {
        SQLiteDatabase db = this.getReadableDatabase();
        int revenue = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(total_bill), 0) FROM sales WHERE date(timestamp) = ?",
                new String[]{todayDate}
        );

        if (cursor.moveToFirst()) {
            revenue = cursor.getInt(0);
        }
        cursor.close();
        return revenue;
    }

    // Get today's total transactions count
    public int getTodayTransactionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM sales WHERE date(timestamp) = ?",
                new String[]{todayDate}
        );

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Get top customers by purchase amount
    public List<String> getTopCustomers(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> customers = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT cust_name, SUM(total_bill) as total FROM sales " +
                        "GROUP BY cust_name ORDER BY total DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        while (cursor.moveToNext()) {
            String customer = cursor.getString(0);
            int total = cursor.getInt(1);
            customers.add(customer + " (KES " + total + ")");
        }
        cursor.close();
        return customers;
    }

    // Get recent customers for quick selection
    public List<String> getRecentCustomers(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> customers = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT cust_name FROM sales " +
                        "ORDER BY timestamp DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        while (cursor.moveToNext()) {
            customers.add(cursor.getString(0));
        }
        cursor.close();
        return customers;
    }

    // Get customer purchase history
    public Cursor getCustomerHistory(String customerName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT product_name, quantity, total_bill, timestamp FROM sales " +
                        "WHERE cust_name = ? ORDER BY timestamp DESC LIMIT 10",
                new String[]{customerName}
        );
    }

    // Calculate today's profit (requires product_details join)
    public int getTodayProfit() {
        SQLiteDatabase db = this.getReadableDatabase();
        int profit = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT SUM((s.selling_price - p.buying_price) * s.quantity) as profit " +
                        "FROM sales s JOIN product_details p ON s.product_name = p.product_name " +
                        "WHERE date(s.timestamp) = ?",
                new String[]{todayDate}
        );

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            profit = cursor.getInt(0);
        }
        cursor.close();
        return profit;
    }

    // Generate transaction reference number
    public String generateTransactionRef() {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmm", Locale.getDefault());
        String timestamp = format.format(new Date());
        return "TXN" + timestamp + String.format("%03d", (int)(Math.random() * 1000));
    }

    // Check low stock products
    public List<String> getLowStockProducts(int threshold) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> lowStockProducts = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT product_name, balance FROM inventory " +
                        "WHERE date(timestamp) = ? AND balance <= ? AND balance > 0",
                new String[]{todayDate, String.valueOf(threshold)}
        );

        while (cursor.moveToNext()) {
            String product = cursor.getString(0);
            int balance = cursor.getInt(1);
            lowStockProducts.add(product + " (" + balance + " left)");
        }
        cursor.close();
        return lowStockProducts;
    }



// ============ NOTEPAD AND NOTES METHODS ==========
// ============ Insert a new note ============
public boolean insertNote(String title, String content) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("title",   title);
    values.put("content", content);

    // Manually set the timestamp to local time instead of relying on DEFAULT
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault());
    String localTime = sdf.format(new java.util.Date());
    values.put("timestamp", localTime);

    long result = db.insert("notes", null, values);
    return result != -1;
}

    // ============ Update an existing note ============
    public boolean updateNote(int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title",   title);
        values.put("content", content);
        // Update timestamp to now
        values.put("timestamp",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault())
                        .format(new java.util.Date()));
        int rows = db.update("notes", values, "id = ?",
                new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // ============ Delete a note ============
    public boolean deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("notes", "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // ============ Get all notes (newest first) ============
    public List<Note> getAllNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Note> notes = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT id, title, content, timestamp FROM notes ORDER BY timestamp DESC",
                null);
        while (cursor.moveToNext()) {
            notes.add(new Note(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)));
        }
        cursor.close();
        return notes;
    }

    // ============ Search notes by title or content ============
    public List<Note> searchNotes(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Note> notes = new ArrayList<>();
        String like = "%" + query + "%";
        Cursor cursor = db.rawQuery(
                "SELECT id, title, content, timestamp FROM notes " +
                        "WHERE title LIKE ? OR content LIKE ? " +
                        "ORDER BY timestamp DESC",
                new String[]{like, like});
        while (cursor.moveToNext()) {
            notes.add(new Note(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)));
        }
        cursor.close();
        return notes;
    }

    // ==================== END OF NOTEPAD AND NOTES METHODS ==========








    // ======================= EXPENSE MANAGEMENT METHODS ============

    // Create expenses table
    public void createExpensesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS expenses(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT," +
                "amount REAL," +
                "date TEXT," +
                "notes TEXT," +
                "timestamp DATETIME DEFAULT (datetime('now', 'localtime')))");
    }

    // Insert expense
    public boolean insertExpense(String category, double amount, String date, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("category", category);
        values.put("amount", amount);
        values.put("date", date);
        values.put("notes", notes);

        long result = db.insert("expenses", null, values);
        return result != -1;
    }

    // Get expenses by date
    public Cursor getExpensesByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM expenses WHERE date = ? ORDER BY timestamp DESC",
                new String[]{date});
    }

    // Get expenses by date range
    public Cursor getExpensesByDateRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM expenses WHERE date BETWEEN ? AND ? ORDER BY timestamp DESC",
                new String[]{startDate, endDate});
    }

    // Get total expenses by date
    public double getTotalExpensesByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date = ?",
                new String[]{date});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Get total expenses by date range
    public double getTotalExpensesByDateRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN ? AND ?",
                new String[]{startDate, endDate});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Delete expense
    public boolean deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("expenses", "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

// ============ FINANCIAL SUMMARY METHODS ============

    /**
     * Get financial summary for a date range
     * @param startDate Start date in yyyy-MM-dd format
     * @param endDate End date in yyyy-MM-dd format
     * @return Cursor with columns: total_revenue, total_profit, total_expenses
     */

    // Get financial summary for a date range
    public Cursor getFinancialSummary(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Using table aliases 's' for sales and 'p' for product_details
        // to avoid ambiguous column name errors
        String query =
                "SELECT " +
                        // Total Revenue from sales
                        "COALESCE((SELECT SUM(total_bill) FROM sales WHERE date(timestamp) BETWEEN ? AND ?), 0) as total_revenue, " +
                        // Total Profit = (selling_price - buying_price) * quantity
                        "COALESCE((SELECT SUM((s.selling_price - p.buying_price) * s.quantity) " +
                        "FROM sales s JOIN product_details p ON s.product_name = p.product_name " +
                        "WHERE date(s.timestamp) BETWEEN ? AND ?), 0) as total_profit, " +
                        // Total Expenses from expenses table
                        "COALESCE((SELECT SUM(amount) FROM expenses WHERE date BETWEEN ? AND ?), 0) as total_expenses";

        return db.rawQuery(query, new String[]{startDate, endDate, startDate, endDate, startDate, endDate});
    }

    // Get top selling products
    public Cursor getTopSellingProducts(String startDate, String endDate, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.product_name, SUM(s.quantity) as total_quantity, SUM(s.total_bill) as total_revenue " +
                        "FROM sales s WHERE date(s.timestamp) BETWEEN ? AND ? " +
                        "GROUP BY s.product_name " +
                        "ORDER BY total_revenue DESC LIMIT ?",
                new String[]{startDate, endDate, String.valueOf(limit)}
        );
    }

    // Get top performing assignees (employees)
    public Cursor getTopAssignees(String startDate, String endDate, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT ig.assignee, COUNT(*) as total_transactions, SUM(ig.quantity) as total_quantity " +
                        "FROM issue_goods ig WHERE date(ig.timestamp) BETWEEN ? AND ? " +
                        "GROUP BY ig.assignee " +
                        "ORDER BY total_quantity DESC LIMIT ?",
                new String[]{startDate, endDate, String.valueOf(limit)}
        );
    }

    // Get daily sales summary
    public Cursor getDailySalesSummary(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " +
                        "COUNT(*) as transaction_count, " +
                        "SUM(total_bill) as total_revenue, " +
                        "AVG(total_bill) as avg_transaction " +
                        "FROM sales WHERE date(timestamp) = ?",
                new String[]{date}
        );
    }

    // Get weekly sales summary
    public Cursor getWeeklySalesSummary(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " +
                        "COUNT(*) as transaction_count, " +
                        "SUM(total_bill) as total_revenue, " +
                        "AVG(total_bill) as avg_transaction, " +
                        "strftime('%w', timestamp) as day_of_week " +
                        "FROM sales WHERE date(timestamp) BETWEEN ? AND ? " +
                        "GROUP BY day_of_week",
                new String[]{startDate, endDate}
        );
    }

// ======================= END OF EXPENSE MANAGEMENT METHODS ============











}
