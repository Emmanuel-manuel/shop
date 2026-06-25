package emm.sys;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.List;

public class ProductShareClient {

    private static final String TAG = "ProductShareClient";
    private static final int PORT = ProductShareServer.PORT;
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 10000;

    public enum ShareResult {
        SUCCESS, DATA_TRANSFER_ERROR, NETWORK_ERROR
    }

    public static class ShareResponse {
        public final ShareResult result;
        public final int inserted;
        public final int skipped;
        public final String detail;

        ShareResponse(ShareResult result, int inserted, int skipped, String detail) {
            this.result = result;
            this.inserted = inserted;
            this.skipped = skipped;
            this.detail = detail;
        }
    }

    public static ShareResponse shareData(Context context, List<?> dataList, String dataType) {
        try {
            String receiverIp = resolveReceiverHost(context);
            if (receiverIp == null) {
                Log.e(TAG, "Could not determine receiver IP");
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Could not find receiver device");
            }

            String baseUrl = "http://" + receiverIp + ":" + PORT;
            Log.d(TAG, "Targeting receiver at: " + baseUrl);

            if (!isReachable(receiverIp)) {
                Log.e(TAG, "Receiver not reachable: " + receiverIp);
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Receiver device not reachable");
            }

            boolean pingSuccess = pingReceiver(baseUrl);
            if (!pingSuccess) {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Receiver app not ready");
            }

            JSONArray json = serializeData(dataList, dataType);
            return postData(baseUrl, json);

        } catch (Exception e) {
            Log.e(TAG, "shareData failed", e);
            return new ShareResponse(ShareResult.NETWORK_ERROR, 0, 0, e.getMessage());
        }
    }

    public static ShareResponse shareProducts(Context context, List<?> productList) {
        return shareData(context, productList, "product");
    }

    public static ShareResponse shareIssuedGoods(Context context, List<?> issuedGoodsList) {
        return shareData(context, issuedGoodsList, "issued_goods");
    }

    private static boolean isReachable(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(2000);
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolveReceiverHost(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;

            int gatewayInt = wm.getDhcpInfo().gateway;
            if (gatewayInt != 0) {
                String gateway = intToIp(gatewayInt);
                Log.d(TAG, "Gateway IP: " + gateway);
                return gateway;
            }

            String[] commonIps = {"192.168.43.1", "192.168.42.1", "192.168.1.1", "172.20.10.1"};
            for (String ip : commonIps) {
                if (isReachable(ip)) {
                    Log.d(TAG, "Found reachable IP: " + ip);
                    return ip;
                }
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "resolveReceiverHost error", e);
            return null;
        }
    }

    private static String intToIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    private static boolean pingReceiver(String baseUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/ping");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.e(TAG, "Ping failed with code: " + code);
                return false;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject json = new JSONObject(sb.toString());
            String status = json.optString("status", "");

            Log.d(TAG, "Ping response: " + status);
            return "ok".equals(status);

        } catch (Exception e) {
            Log.e(TAG, "pingReceiver error", e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static JSONArray serializeData(List<?> dataList, String dataType) throws Exception {
        JSONArray array = new JSONArray();
        for (Object obj : dataList) {
            JSONObject p = new JSONObject();

            if (dataType.equals("product")) {
                p.put("product_name", invokeGetter(obj, "getProductName"));
                p.put("weight", invokeGetter(obj, "getWeight"));
                p.put("flavour", invokeGetter(obj, "getFlavour"));
                p.put("buying_price", invokeGetter(obj, "getBuyingPrice"));
                p.put("selling_price", invokeGetter(obj, "getSellingPrice"));
                p.put("profit", invokeGetter(obj, "getProfit"));
                p.put("timestamp", invokeGetter(obj, "getTimestamp"));
            } else if (dataType.equals("issued_goods")) {
                p.put("assignee", invokeGetter(obj, "getAssignee"));
                p.put("product_name", invokeGetter(obj, "getProductName"));
                p.put("weight", invokeGetter(obj, "getWeight"));
                p.put("flavour", invokeGetter(obj, "getFlavour"));
                p.put("quantity", invokeGetter(obj, "getQuantity"));
                p.put("station", invokeGetter(obj, "getStation"));
                p.put("timestamp", invokeGetter(obj, "getTimestamp"));
            }

            array.put(p);
        }
        return array;
    }

    private static Object invokeGetter(Object obj, String methodName) throws Exception {
        return obj.getClass().getMethod(methodName).invoke(obj);
    }

    private static ShareResponse postData(String baseUrl, JSONArray json) {
        HttpURLConnection conn = null;
        try {
            byte[] body = json.toString().getBytes("UTF-8");
            URL url = new URL(baseUrl + "/receive");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));

            conn.connect();
            OutputStream os = conn.getOutputStream();
            os.write(body);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream inputStream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject response = new JSONObject(sb.toString());
            String status = response.optString("status", "error");

            if ("ok".equals(status)) {
                int inserted = response.optInt("inserted", 0);
                int skipped = response.optInt("skipped", 0);
                return new ShareResponse(ShareResult.SUCCESS, inserted, skipped, "Transfer complete");
            } else {
                return new ShareResponse(ShareResult.DATA_TRANSFER_ERROR, 0, 0,
                        "Receiver error: " + status);
            }

        } catch (Exception e) {
            Log.e(TAG, "postData error", e);
            return new ShareResponse(ShareResult.NETWORK_ERROR, 0, 0, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}