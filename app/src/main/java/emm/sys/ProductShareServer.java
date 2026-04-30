package emm.sys;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * ProductShareServer
 *
 * Runs a lightweight HTTP server on the receiver device (the one with hotspot ON).
 * Listens on port 8765 for incoming product data from another device running the same app.
 *
 * Endpoints:
 *   GET  /ping          — Health check; also verifies the local DB has the required table/schema.
 *   POST /receive       — Accepts a JSON array of products and inserts them into local SQLite DB.
 *
 * Usage:
 *   ProductShareServer server = new ProductShareServer(context);
 *   server.start();
 *   // ... later ...
 *   server.stop();
 */
public class ProductShareServer extends NanoHTTPD {

    private static final String TAG = "ProductShareServer";
    public static final int PORT = 8765;

    // The /ping response includes this token so the client can confirm it's talking
    // to the same app (not just any HTTP server on the network).
    public static final String APP_TOKEN = "EMM_SALES_APP_V1";

    private final Context context;
    private final DBHelper dbHelper;

    public ProductShareServer(Context context) {
        super(PORT);
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // ----------------------------------------------------------------
            // GET /ping  — health check + schema validation
            // ----------------------------------------------------------------
            if (Method.GET.equals(method) && "/ping".equals(uri)) {
                return handlePing();
            }

            // ----------------------------------------------------------------
            // POST /receive  — receive and store product data
            // ----------------------------------------------------------------
            if (Method.POST.equals(method) && "/receive".equals(uri)) {
                return handleReceive(session);
            }

            // Unknown endpoint
            return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Unknown endpoint\"}"
            );

        } catch (Exception e) {
            Log.e(TAG, "Server error", e);
            return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Server internal error\"}"
            );
        }
    }

    // ------------------------------------------------------------------
    // /ping handler
    // ------------------------------------------------------------------
    private Response handlePing() {
        boolean schemaOk = verifyDatabaseSchema();
        if (!schemaOk) {
            return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"status\":\"schema_error\",\"token\":\"" + APP_TOKEN + "\"}"
            );
        }
        return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                "{\"status\":\"ok\",\"token\":\"" + APP_TOKEN + "\"}"
        );
    }

    // ------------------------------------------------------------------
    // /receive handler
    // ------------------------------------------------------------------
    private Response handleReceive(IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
        // Read request body
        Map<String, String> files = new java.util.HashMap<>();
        session.parseBody(files);
        String body = files.get("postData");

        if (body == null || body.isEmpty()) {
            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Empty body\"}"
            );
        }

        // Validate schema before attempting insert
        if (!verifyDatabaseSchema()) {
            return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"status\":\"schema_error\",\"message\":\"Schema mismatch\"}"
            );
        }

        try {
            JSONArray products = new JSONArray(body);
            int inserted = 0;
            int skipped = 0;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);

                    String productName  = p.getString("product_name");
                    String weight       = p.getString("weight");
                    String flavour      = p.getString("flavour");
                    int buyingPrice     = p.getInt("buying_price");
                    int sellingPrice    = p.getInt("selling_price");
                    int profit          = p.getInt("profit");
                    String timestamp    = p.getString("timestamp");

                    // Skip duplicate (same name + weight + flavour)
                    boolean exists = dbHelper.checkProductDetailsExists(productName, weight, flavour);
                    if (exists) {
                        skipped++;
                        continue;
                    }

                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("product_name", productName);
                    values.put("weight", weight);
                    values.put("flavour", flavour);
                    values.put("buying_price", buyingPrice);
                    values.put("selling_price", sellingPrice);
                    values.put("profit", profit);
                    values.put("timestamp", timestamp);

                    long result = db.insert("product_details", null, values);
                    if (result != -1) inserted++;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            JSONObject response = new JSONObject();
            response.put("status", "ok");
            response.put("inserted", inserted);
            response.put("skipped", skipped);

            return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    response.toString()
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to process received products", e);
            return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Failed to process data\"}"
            );
        }
    }

    // ------------------------------------------------------------------
    // Schema verification
    // Checks that the local SQLite DB has a 'product_details' table with
    // the expected columns. If not → the receiver is incompatible.
    // ------------------------------------------------------------------
    private boolean verifyDatabaseSchema() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            // This will throw if the table doesn't exist
            android.database.Cursor cursor = db.rawQuery(
                    "SELECT id, product_name, weight, flavour, buying_price, selling_price, profit, timestamp " +
                            "FROM product_details LIMIT 0", null
            );
            cursor.close();
            return true;
        } catch (SQLiteException e) {
            Log.w(TAG, "Schema verification failed: " + e.getMessage());
            return false;
        }
    }
}
