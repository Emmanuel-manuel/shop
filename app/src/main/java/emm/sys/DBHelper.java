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

        super(context, "Shop.db", null, 8);
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
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

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
            // Update inventory balance with the new balance value
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




















}
