package emm.sys;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.List;

/**
 * ProductShareClient
 *
 * Runs on the SENDER device. Discovers the hotspot-host device on the LAN,
 * validates that the receiver has the same app + compatible DB schema installed,
 * then POSTs the product list as JSON.
 *
 * Flow:
 *   1. resolveReceiverHost()  — derives the gateway IP (hotspot host) from DHCP info
 *   2. pingReceiver()         — GET /ping to confirm app token + schema OK
 *   3. sendProducts()         — POST /receive with JSON payload
 *
 * All methods are blocking — call from a background thread.
 */
public class ProductShareClient {

    private static final String TAG = "ProductShareClient";
    private static final int PORT = ProductShareServer.PORT;           // 8765
    private static final int CONNECT_TIMEOUT_MS = 6_000;
    private static final int READ_TIMEOUT_MS    = 10_000;

    // ----------------------------------------------------------------
    // Public result contract
    // ----------------------------------------------------------------
    public enum ShareResult {
        SUCCESS,
        DATA_TRANSFER_ERROR,   // Receiver not found, wrong app, or schema mismatch
        NETWORK_ERROR          // General IO / timeout
    }

    public static class ShareResponse {
        public final ShareResult result;
        public final int inserted;
        public final int skipped;
        public final String detail;   // Human-readable detail for logging

        ShareResponse(ShareResult result, int inserted, int skipped, String detail) {
            this.result   = result;
            this.inserted = inserted;
            this.skipped  = skipped;
            this.detail   = detail;
        }
    }

    // ----------------------------------------------------------------
    // Entry point — called from ViewProductsDetailsFragment background thread
    // ----------------------------------------------------------------
    public static ShareResponse shareProducts(Context context, List<?> productList) {
        try {
            String receiverIp = resolveReceiverHost(context);
            if (receiverIp == null) {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Could not determine hotspot gateway IP");
            }

            String baseUrl = "http://" + receiverIp + ":" + PORT;
            Log.d(TAG, "Targeting receiver at: " + baseUrl);

            // Step 1: ping
            PingResult ping = pingReceiver(baseUrl);
            if (ping == PingResult.NOT_FOUND || ping == PingResult.SCHEMA_ERROR) {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Ping result: " + ping.name());
            }
            if (ping == PingResult.NETWORK_ERROR) {
                return new ShareResponse(ShareResult.NETWORK_ERROR, 0, 0,
                        "Network error during ping");
            }

            // Step 2: serialize products
            JSONArray json = serializeProducts(productList);

            // Step 3: POST
            return postProducts(baseUrl, json);

        } catch (Exception e) {
            Log.e(TAG, "shareProducts failed", e);
            return new ShareResponse(ShareResult.NETWORK_ERROR, 0, 0, e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Resolve the hotspot gateway IP
    //
    // When this device is connected to a mobile hotspot, the DHCP gateway
    // is the hotspot host. We read it from WifiManager.
    // ----------------------------------------------------------------
    private static String resolveReceiverHost(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;

            int gatewayInt = wm.getDhcpInfo().gateway;
            if (gatewayInt == 0) return null;

            // Android stores IP as little-endian int; convert to string
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                gatewayInt = Integer.reverseBytes(gatewayInt);
            }
            return InetAddress.getByAddress(
                    new byte[]{
                            (byte)(gatewayInt >> 24),
                            (byte)(gatewayInt >> 16),
                            (byte)(gatewayInt >> 8),
                            (byte)(gatewayInt)
                    }
            ).getHostAddress();

        } catch (Exception e) {
            Log.e(TAG, "resolveReceiverHost error", e);
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Ping the receiver to confirm:
    //   a) The EMM Sales App is installed and running the server
    //   b) The receiver's DB schema is compatible
    // ----------------------------------------------------------------
    private enum PingResult { OK, NOT_FOUND, SCHEMA_ERROR, NETWORK_ERROR }

    private static PingResult pingReceiver(String baseUrl) {
        try {
            URL url = new URL(baseUrl + "/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.connect();

            int code = conn.getResponseCode();
            if (code != 200) {
                return PingResult.NOT_FOUND;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();

            JSONObject json = new JSONObject(sb.toString());
            String token  = json.optString("token", "");
            String status = json.optString("status", "");

            // Confirm app token matches
            if (!ProductShareServer.APP_TOKEN.equals(token)) {
                Log.w(TAG, "Token mismatch: " + token);
                return PingResult.NOT_FOUND;
            }

            if ("schema_error".equals(status)) {
                return PingResult.SCHEMA_ERROR;
            }

            return "ok".equals(status) ? PingResult.OK : PingResult.NOT_FOUND;

        } catch (java.net.ConnectException | java.net.SocketTimeoutException e) {
            Log.w(TAG, "Receiver not reachable: " + e.getMessage());
            return PingResult.NOT_FOUND;
        } catch (Exception e) {
            Log.e(TAG, "pingReceiver error", e);
            return PingResult.NETWORK_ERROR;
        }
    }

    // ----------------------------------------------------------------
    // Serialize product list to JSON array
    // Works with ViewProductsDetailsFragment.Product (accessed via
    // reflection-safe getters defined in the inner class).
    // ----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static JSONArray serializeProducts(List<?> productList) throws Exception {
        JSONArray array = new JSONArray();
        for (Object obj : productList) {
            // Use reflection to call getters — keeps this class decoupled
            // from the inner Product class in ViewProductsDetailsFragment.
            JSONObject p = new JSONObject();
            p.put("product_name",  invokeGetter(obj, "getProductName"));
            p.put("weight",        invokeGetter(obj, "getWeight"));
            p.put("flavour",       invokeGetter(obj, "getFlavour"));
            p.put("buying_price",  invokeGetter(obj, "getBuyingPrice"));
            p.put("selling_price", invokeGetter(obj, "getSellingPrice"));
            p.put("profit",        invokeGetter(obj, "getProfit"));
            p.put("timestamp",     invokeGetter(obj, "getTimestamp"));
            array.put(p);
        }
        return array;
    }

    private static Object invokeGetter(Object obj, String methodName) throws Exception {
        return obj.getClass().getMethod(methodName).invoke(obj);
    }

    // ----------------------------------------------------------------
    // POST /receive with the JSON payload
    // ----------------------------------------------------------------
    private static ShareResponse postProducts(String baseUrl, JSONArray json) {
        try {
            byte[] body = json.toString().getBytes("UTF-8");

            URL url = new URL(baseUrl + "/receive");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            OutputStream os = conn.getOutputStream();
            os.write(body);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            code == 200 ? conn.getInputStream() : conn.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();

            JSONObject response = new JSONObject(sb.toString());
            String status = response.optString("status", "error");

            if ("ok".equals(status)) {
                int inserted = response.optInt("inserted", 0);
                int skipped  = response.optInt("skipped", 0);
                return new ShareResponse(ShareResult.SUCCESS, inserted, skipped, "Transfer complete");
            } else if ("schema_error".equals(status)) {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Receiver schema mismatch");
            } else {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Receiver returned error: " + status);
            }

        } catch (Exception e) {
            Log.e(TAG, "postProducts error", e);
            return new ShareResponse(ShareResult.NETWORK_ERROR, 0, 0, e.getMessage());
        }
    }
}
