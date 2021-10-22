package com.att;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Scanner;

public class CobaTwinTest {

    public static final String BEARER = "Bearer";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TOKEN_CONTENT_VALUE = "application/x-www-form-urlencoded";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String RESOURCE = "resource";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String AZURE_CLIENT_ID = "AzureClientId";
    public static final String AZURE_CLIENT_SECRET = "AzureClientSecret";
    public static final String AZURE_CLIENT_RESOURCE = "AzureClientResource";
    public static final String COBA_TWIN_REPORT_URL = "CobaTwinReportUrl";
    public static final String VCS_MICROSOFT_ACCESS_TOKEN = "VCSMicrosoftAccessToken";
    public static final String ICCID_NUMBER = "iccidNumber";
    public static final String PROXY_HOSTNAME = "appproxy.us164.corpintra.net";
    public static final Integer PROXY_PORT = 3128;

    private static String getAccessToken(MultiValueMap<String, String> bodyMap, String tokenUrl,
            boolean isUrlBehindTheProxy) throws Exception {
        System.out.println("Started fetching token");
        RestTemplate restTemplate = getRestTemplate(isUrlBehindTheProxy);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyMap, headers);

        JSONObject jsonObject = new JSONObject(restTemplate.postForObject(tokenUrl, requestEntity, String.class));
        System.out.println("Access Token JSONObject:" + jsonObject);
        return jsonObject.getString("access_token");
    }

    private static HttpHeaders getCobaHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.println(String.format("%s %s", "Bearer", token));
        headers.add(HttpHeaders.AUTHORIZATION, String.format("%s %s", "Bearer", token));
        return headers;
    }

    public static ResponseEntity<String> fetchICCIDFromCoBa(MultiValueMap<String, String> bodyMap, String tokenUrl,
            String path, String deviceId, boolean isProxy) throws Exception {

        HttpHeaders headers = getCobaHttpHeaders(getAccessToken(bodyMap, tokenUrl, isProxy));

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        String uri = path.replace("{deviceId}", deviceId);
        System.out.println(String.format("fetchICCIDFromCoBa->URI :{%s}, headers:{%s}", uri, headers));
        RestTemplate restTemplate = getRestTemplate(isProxy);

        return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
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

    public static void main(String[] args) throws Exception {
        String cobaTwinReportUrl = "https://api.staging.us.coba.daimler.com/dvmgmt/v1/devices/ctp-{deviceId}/twin";
        String tokenUrl = "https://login.microsoftonline.com/9652d7c2-1ccf-4940-8151-4a92bd474ed0/oauth2/token";

        MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        bodyMap.add(CLIENT_ID, "16a77ce8-6956-4e93-8edd-dae1f2aa9820");
        bodyMap.add(CLIENT_SECRET, "4rMfpyzNB4B/O[i]=I2ZjtxSD8G0@iXc");
        bodyMap.add(RESOURCE, "939d143c-c4ff-4433-b879-57c70e2891d5");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Do you want to run with Proxy: true/false");
        Boolean isProxy = scanner.nextBoolean();
        ResponseEntity<String> response = fetchICCIDFromCoBa(bodyMap, tokenUrl, cobaTwinReportUrl, "3630006028",
                isProxy);
        System.out.println(response);
    }
}
