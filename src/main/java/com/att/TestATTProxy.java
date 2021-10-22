package com.att;

import com.google.gson.Gson;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class TestATTProxy {

    private static final Gson gson = new Gson();
    public static final String PROXY_HOSTNAME = "appproxy.us164.corpintra.net";
    public static final Integer PROXY_PORT = 3128;
    public static final String USER_AGENT = "User-Agent";
    public static final String ACTIVATED = "ACTIVATED";
    public static final String DEACTIVATED = "DEACTIVATED";
    public static final String PUT = "PUT";
    public static final String STATUS = "status";
    public static final String POST = "POST";
    public static final String CONNECTION = "Connection";
    public static final String KEEP_ALIVE = "keep-alive";
    public static final String GZIP_DEFLATE_BR = "gzip,deflate,br";
    public static final String NO_CACHE = "no-cache";
    public static final String STRING_STAR = "*/*";
    public static final String CMS_DEACTIVATION_PROCESS_URL = "CMSDeactivationProcessUrl";
    public static final String PROCESS = "process";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    static byte[] vals = {(byte) 0xca, 0x3c, 0x55, (byte) 0xc3, (byte) 0xaa, (byte) 0x96, 0x65};
    private static final String USER_AGENT1 = "Mozilla/5.0";
    private static final String attprops = orig(
            "i0khq8XkDLBdIarF+F+IXSaqybYw+Gosp8f6D5BqA7nwziy9cRGOnMwPj0Ub8ePhKKBtIZnuwx+Tb2Xz5PwBpnABqNDMIp9ID5Tj7jyddjiN/v1Rkw4Y85HVCqRIMK3euzGzTDD5y+YVplU2ot7/CqQTP7DF+F6JUzqow/NfiHUSqtrFALhKMLHU1QqkSCesxskmr1IhptjJV7RsGozmyTWFeGSc+ME29x0emoWkBpJpNLPy+imuEyapnKMfhkowoMHDEoByHLHe1CmeSyCrzfQ1gA04ttLvJrMFLbrtxgmACj+l+KYrrU0Dk8vyAfwEG/bDwz+taQCJ6dkqjVkf9v/kN797APrJ+RaTdh+y5/hQjEk9rZjBEvcB");

    private static String orig(String v) {
        Base64.Decoder d = Base64.getDecoder();
        byte[] b = d.decode(v);
        for (int i = 0; i < b.length; i++) {
            b[i] ^= vals[i % vals.length];
        }
        return new String(b);
    }
    ////////////////////////////////////////////////////////////////////
    // AT&T Helpers
    ////////////////////////////////////////////////////////////////////

    private static ResponseEntity<String> getAttStatus(String iccid) {
        return getATT(iccid, "iccid,accountCustom1,deviceID,status");
    }

    private static ResponseEntity<String> putATT(String iccid, String body) {
        String atturl = String.format("https://api-iotdevice.att.com/rws/api/v1/devices/%s", iccid);
        return putHTTPS(atturl, attprops, body);
    }

    private static ResponseEntity<String> getATT(String iccid, String fields) {
        String baseurl = "https://api-iotdevice.att.com/rws/api/v1/devices";
        String atturl = String.format("%s/%s%s", baseurl, iccid, fields == null ? "" : "?fields=" + fields);
        return getHTTPS(atturl, attprops);
    }

    private static ResponseEntity<String> setAttStatus(String iccid, boolean enabled) {
        System.out.println(String.format("Updating AT&T[%s].enabled = '%s'", iccid, enabled));
        String body = String.format("{\"status\":\"%s\"}", enabled ? "ACTIVATED" : "DEACTIVATED");
        return putATT(iccid, body);
    }
    ////////////////////////////////////////////////////////////////////
    // HTTPS Helpers
    ////////////////////////////////////////////////////////////////////

    private static ResponseEntity<String> getHTTPS(String httpsURL, String properties) {
        return doHTTPS("GET", httpsURL, properties, null);
    }

    private static ResponseEntity<String> putHTTPS(String httpsURL, String properties, String body) {
        return doHTTPS("PUT", httpsURL, properties, body);
    }

    private static ResponseEntity<String> postHTTPS(String httpsURL, String properties, String body) {
        return doHTTPS("POST", httpsURL, properties, body);
    }

    private static ResponseEntity<String> deleteHTTPS(String httpsURL, String properties, String body) {
        return doHTTPS("DELETE", httpsURL, properties, body);
    }

    private static String lastHttpsResp;

    private static ResponseEntity<String> doHTTPS(String RequestType, String httpsURL, String properties, String body) {
        String response;
        int respCode = 500;
        try {
            URL myUrl = new URL(httpsURL);
            HttpsURLConnection  conn = (HttpsURLConnection) myUrl.openConnection(
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress("appproxy.us164.corpintra.net", 3128)));
            conn.setHostnameVerifier((hostname, session) -> true);

            if (properties != null && properties.length() > 0) {
                String[] props = properties.split(";");
                for (String prop : props) {
                    String[] vals = prop.split(":");
                    conn.addRequestProperty(vals[0], vals[1]);
                }
            }
            conn.setRequestMethod(RequestType);

            if (body != null) {
                conn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(body);
                wr.flush();
                wr.close();
            }

            conn.connect();
            respCode = conn.getResponseCode();
            response = String.format("Resp = %d: %s performing %s", respCode, conn.getResponseMessage(), httpsURL);
            if (respCode == 200) {
                response = "";
                InputStream is = conn.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    response += inputLine;
                }
                br.close();
                System.out.println("ATT SIM has been deactivated successfully:{}"+ response);
                return new ResponseEntity<>(response,HttpStatus.OK);
            }
        } catch (Exception e) {
            System.out.println(
                    String.format("Exception happened while SIM Deactivating in ATT:"+e));
            return new ResponseEntity<>(HttpStatus.valueOf(respCode));
        }
        return new ResponseEntity<>(HttpStatus.valueOf(respCode));
    }

    private static String doHTTPS1(String RequestType, String httpsURL, String properties, String body) {
        String ret = "";
        lastHttpsResp = null;
        try {
            URL myUrl = new URL(httpsURL);
            HttpsURLConnection conn = null;

            //            conn = (HttpsURLConnection) myUrl.openConnection();
            conn = (HttpsURLConnection) myUrl.openConnection(
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress("appproxy.us164.corpintra.net", 3128)));
//            conn.setHostnameVerifier((hostname, session) -> true);

            if (properties != null && properties.length() > 0) {
                String[] props = properties.split(";");
                for (String prop : props) {
                    String[] vals = prop.split(":");
                    conn.addRequestProperty(vals[0], vals[1]);
                }
            }
            //            conn.setUseCaches(false);
            //            conn.setRequestProperty("User-Agent", USER_AGENT);
            //            conn.setRequestProperty("Accept", "*/*");
            //            conn.setRequestProperty("Accept-Encoding", "gzip,deflate,br");
            //            conn.setRequestProperty("Connection", "keep-alive");
            //            conn.setDoInput(true);
            conn.setRequestMethod(RequestType); // GET/POST/DELETE

            if (body != null) {
                conn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(body);
                wr.flush();
                wr.close();
            }

            conn.connect();
            int respCode = conn.getResponseCode();
            lastHttpsResp = String.format("Resp = %d: %s performing %s", respCode, conn.getResponseMessage(), httpsURL);
            if (respCode >= 200 && respCode <= 299) {
                InputStream is = conn.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    ret += inputLine;
                }
                br.close();
            }
            System.out.println(String.format("%s", lastHttpsResp));
            System.out.println(String.format("%s", ret));
        } catch (Exception e) {
            System.out.println(String.format("Threw %s", e.toString()));
        }
        return ret;
    }

    private static HttpHeaders getHttpHeaders(String properties) {
        HttpHeaders headers = new HttpHeaders();
        //        headers.setCacheControl(NO_CACHE);
        //        headers.add(USER_AGENT, USER_AGENT1);
        //        headers.add(ACCEPT, STRING_STAR);
        //        headers.add(ACCEPT_ENCODING, GZIP_DEFLATE_BR);
        //        headers.add(CONNECTION, KEEP_ALIVE);
        headers.setContentType(APPLICATION_JSON);

        if (properties != null && properties.length() > 0) {

            String[] props = properties.split(";");
            Arrays.stream(props).forEach(prop -> {
                String[] vals = prop.split(":");
                headers.add(vals[0], vals[1]);
            });
        }
        return headers;
    }

    private static RestTemplate getRestTemplate(boolean isUrlBehindTheProxy) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        RestTemplate restTemplate;
        if (isUrlBehindTheProxy) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOSTNAME, PROXY_PORT));
            requestFactory.setProxy(proxy);
            restTemplate = new RestTemplate(requestFactory);
        } else {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }

    public static ResponseEntity<String> putRequest(String httpsURL, String properties, String body,
            Boolean isUrlBehindTheProxy) throws Exception {

        HttpHeaders headers = getHttpHeaders(properties);
        ResponseEntity<String> response;
        System.out.println(String.format("RequestBody :%s and Headers:%s", body, headers));
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = getRestTemplate(isUrlBehindTheProxy);
        response = restTemplate.exchange(httpsURL, HttpMethod.PUT, requestEntity, String.class);
        return response;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("start");
        System.out.println("=================Normal URLConnection Approach===========================");
        boolean status;
        if (getAttStatus("89011704278099010201").getBody().contains("DEACTIVATED")) {
            status = true;
        } else {
            status = false;
        }
        System.out.println(getAttStatus("89011704278099010201"));
        System.out.println(setAttStatus("89011704278099010201", status));
        System.out.println(getAttStatus("89011704278099010201"));

        String fields = "iccid,accountCustom1,deviceID,status";
        String baseurl = "https://api-iotdevice.att.com/rws/api/v1/devices";
        String connString = "Authorization:Basic U2VydmljZVVzZXIwMDM6ZjEyN2IwMjQtZDUzYS00NjdlLTkzZGUtZWIxYWJmNTk4Y2M0;Cookie:BIGipServer~Control_Center_2~POOL_POD1_RWS=!KY/2cXUapXlLd/sj65zLveckUwJNIrtBLTwuhgbPJ1muxyCy9xyGPlJ6jfR0NgqVPadd68N5iUZgUUJCOOGeJ5UrRuGU9cosYJJqMn5Fuhn2Ww==";
        String atturl = String
                .format("%s/%s%s", baseurl, "89011704278099010201", fields == null ? "" : "?fields=" + fields);

        System.out.println("=================RestTemplate Approach===========================");
        if (getAttStatus("89011704278099010201").getBody().contains("DEACTIVATED")) {
            status = true;
        } else {
            status = false;
        }
        Map<String, String> body = new HashMap<>();
        if (status) {
            body.put(STATUS, ACTIVATED);
        } else {
            body.put(STATUS, DEACTIVATED);
        }
        System.out.println(getAttStatus("89011704278099010201"));
        ResponseEntity responseEntity = putRequest(
                "https://api-iotdevice.att.com/rws/api/v1/devices/89011704278099010201", connString, gson.toJson(body),
                true);
        System.out.println(getAttStatus("89011704278099010201"));

    }

}
