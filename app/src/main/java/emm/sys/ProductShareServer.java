package emm.sys;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
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
    private ConnectionListener connectionListener;

    public interface ConnectionListener {
        void onDeviceConnected();
        void onDataReceived(int productCount);
    }

    public ProductShareServer(Context context) {
        super(PORT);
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.d(TAG, "Server started on port " + PORT);
//        // Log the server's IP address for debugging
//        String ip = getLocalIpAddress();
//        Log.d(TAG, "Server IP address: " + ip);
//
//        // Show toast on the device
//        if (context != null) {
//            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
//            mainHandler.post(() -> {
//                Toast.makeText(context,
//                        "Share Server Running\nIP: " + ip + ":" + PORT,
//                        Toast.LENGTH_LONG).show();
//            });
//        }
    }

//    private String getLocalIpAddress() {
//        try {
//            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
//            for (NetworkInterface intf : interfaces) {
//                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
//                for (InetAddress addr : addrs) {
//                    if (!addr.isLoopbackAddress()) {
//                        String sAddr = addr.getHostAddress();
//                        boolean isIPv4 = sAddr.indexOf(':') < 0;
//                        if (isIPv4) {
//                            return sAddr;
//                        }
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            Log.e(TAG, "Error getting IP address", ex);
//        }
//        return "Unknown";
//    }


    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        Log.d(TAG, "Received request: " + method + " " + uri);

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
    private Response handleReceive(IHTTPSession session) throws IOException {
        Log.d(TAG, "Handling receive request");

        if (connectionListener != null) {
            connectionListener.onDeviceConnected();
        }

        // Read request body
        Map<String, String> files = new java.util.HashMap<>();
        try {
            session.parseBody(files);
        } catch (ResponseException e) {
            Log.e(TAG, "Failed to parse body", e);
            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Failed to parse request body\"}"
            );
        }

        String body = files.get("postData");
        Log.d(TAG, "Received body length: " + (body != null ? body.length() : 0));

        if (body == null || body.isEmpty()) {
            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Empty body\"}"
            );
        }

        if (!verifyDatabaseSchema()) {
            return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"status\":\"schema_error\",\"message\":\"Database schema mismatch\"}"
            );
        }

        try {
            JSONArray products = new JSONArray(body);
            Log.d(TAG, "Received " + products.length() + " items");

            int inserted = 0;
            int skipped = 0;

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);

                    // Check if this is a product or issued goods item
                    if (p.has("product_name") && !p.has("assignee")) {
                        // Product data
                        String productName  = p.getString("product_name");
                        String weight       = p.getString("weight");
                        String flavour      = p.getString("flavour");
                        int buyingPrice     = p.getInt("buying_price");
                        int sellingPrice    = p.getInt("selling_price");
                        int profit          = p.getInt("profit");
                        String timestamp    = p.getString("timestamp");

                        // Check if product already exists
                        boolean exists = dbHelper.checkProductDetailsExists(productName, weight, flavour);
                        if (exists) {
                            skipped++;
    //                        Log.d(TAG, "Skipping existing product: " + productName);
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
                        } else if (p.has("assignee") && p.has("product_name")) {
                            // Issued goods data
                            String assignee = p.getString("assignee");
                            String productName = p.getString("product_name");
                            String weight = p.getString("weight");
                            String flavour = p.getString("flavour");
                            int quantity = p.getInt("quantity");
                            String station = p.getString("station");
                            String timestamp = p.getString("timestamp");

                            // Check for duplicate
                            boolean exists = dbHelper.checkDuplicateIssue(assignee, productName, weight, flavour, station);
                            if (exists) {
                                skipped++;
                                continue;
                            }

                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put("assignee", assignee);
                            values.put("product_name", productName);
                            values.put("weight", weight);
                            values.put("flavour", flavour);
                            values.put("quantity", quantity);
                            values.put("station", station);
                            values.put("timestamp", timestamp);

                            long result = db.insert("issue_goods", null, values);
                            if (result != -1) inserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Transaction successful. Inserted: " + inserted + ", Skipped: " + skipped);
                } finally {
                    db.endTransaction();
                }

                if (connectionListener != null && inserted > 0) {
                    connectionListener.onDataReceived(inserted);
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
                Log.e(TAG, "Failed to process received items", e);
                return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"status\":\"error\",\"message\":\"Failed to process data: " + e.getMessage() + "\"}"
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
            Log.d(TAG, "Database schema verification successful");
            return true;
        } catch (SQLiteException e) {
            Log.w(TAG, "Schema verification failed: " + e.getMessage());
            return false;
        }
    }
}
