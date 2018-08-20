package com.example.space.hkm.rest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.text.TextUtils;
import android.util.Base64;

import com.example.space.hkm.data.Model;
import com.example.space.hkm.helpers.Logger;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

//import org.azavea.helpers.Logger;
import com.example.space.hkm.App;
//import org.azavea.otm.data.Model;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;

// This class is designed to take care of the base-url
// and otm api-key for REST requests
public class RestClient {
    private static final int NUM_OF_RETRIES = 3;
    private static final int TIMEOUT_IN_MILLIS = 4000;  // 4 seconds
    private static final int TIMEOUT_BETWEEN_RETRIES = 1500;  // 1.5 seconds

    private final String apiUrl;

    private final String baseUrl;

    private String host;

    private AsyncHttpClient client;

    private final SharedPreferences prefs;

    private final String appVersion;

    private final RequestSignature reqSigner;

    public RestClient() {
        prefs = App.getSharedPreferences();
        apiUrl = getApiUrl();
        baseUrl = getBaseUrl();
        appVersion = getAppVersion();
        client = createHttpClient();
        reqSigner = new RequestSignature(prefs.getString("secret_key", ""));

        // The underlying request mechanism doesn't appear to set the HOST
        // header correctly, so include the header manually - it is required
        // to generate a matching signature on the api server.
        try {
            // Authority is servername[:port] if port is not 80
            host = new URI(apiUrl).getAuthority();
        } catch (URISyntaxException e) {
//            Logger.error(apiUrl+"Hellooeoeoeo");
//            Logger.error("Could not determine valid HOST from base URL", e);
        }
    }

    private AsyncHttpClient createHttpClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("platform-ver-build", appVersion);
        client.setTimeout(TIMEOUT_IN_MILLIS);
        client.setMaxRetriesAndTimeout(NUM_OF_RETRIES, TIMEOUT_BETWEEN_RETRIES);
        return client;
    }

    private String getApiUrl() {
        String apiUrl = prefs.getString("api_url", "");
        return apiUrl;
    }

    private String getBaseUrl() {
        String baseUrl = prefs.getString("base_url", "192.168.1.143:7070");
        return baseUrl;
    }

    /**
     * Signed GET request with basic authentication headers
     */
    public void getWithAuthentication(String url, String username,
                                      String password, RequestParams params,
                                      AsyncHttpResponseHandler responseHandler) {

        Header[] authHeader =
                {createBasicAuthenticationHeader(username, password)};
        this.get(url, params, new ArrayList<>(Arrays.asList(authHeader)),
                responseHandler);
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return safePathJoin(apiUrl, relativeUrl);
    }

    private String safePathJoin(String base, String path) {
        String cleanBase = base;
        String cleanPath = path;
        if (base.charAt(base.length() - 1) == '/') {
            cleanBase = base.substring(0, base.length() - 1);
        }

        if (!TextUtils.isEmpty(path) && path.charAt(0) == '/') {
            cleanPath = path.substring(1);
        } else if (TextUtils.isEmpty(path)) {
            return cleanBase;
        }
        return cleanBase + "/" + cleanPath;
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormatUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormatUtc.format(new Date());
    }

    private String getTimestampQuery() {
        return "timestamp=" + getTimestamp();
    }

    /**
     * Configured Access Key for API request verification
     *
     * @return Query string format of "access_key={ACCESSKEY}"
     */
    private String getAccessKeyQuery() {
        return "access_key=" + getAccessKey();
    }

    private String getAccessKey() {
        return prefs.getString("access_key", "");
    }


    private RequestParams prepareParams(RequestParams params) {
        // We'll always need a RequestParams object since we'll always
        // be sending credentials
        RequestParams reqParams;
        if (params == null) {
            reqParams = new RequestParams();
        } else {
            reqParams = params;
        }

        reqParams.put("timestamp", getTimestamp());
        reqParams.put("access_key", getAccessKey());

        return reqParams;
    }

    private void get(String url, RequestParams params,
                     ArrayList<Header> headers, AsyncHttpResponseHandler responseHandler) {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        String reqUrl = getAbsoluteUrl(url);
        RequestParams reqParams = prepareParams(params);
        try {
            headers.add(reqSigner.getSignatureHeader("GET", reqUrl, reqParams));
        } catch (UnsupportedEncodingException | URISyntaxException | SignatureException e) {
//            Logger.error("Failure making GET request", e);
            return;
        }

        Header[] fullHeaders = prepareHeaders(headers);
        client.get(App.getAppInstance(), reqUrl, fullHeaders, reqParams, responseHandler);
    }

    /**
     * Signed GET request with no authentication
     */
    public void get(String url, RequestParams params,
                    AsyncHttpResponseHandler responseHandler) {

        this.get(url, params, null, responseHandler);
    }


    /**
     * Ensure all required headers are present
     *
     * @param additionalHeaders List of headers specific to a single request
     * @return Complete list of headers necessary for API request
     */
    private Header[] prepareHeaders(ArrayList<Header> additionalHeaders) {
        BasicHeader defaultHeader = new BasicHeader("Host", host);

        if (additionalHeaders != null) {
            ArrayList<Header> headers =
                    (ArrayList<Header>) additionalHeaders.clone();
            headers.add(defaultHeader);

            return headers.toArray(new Header[headers.size()]);
        } else {
            return new Header[]{defaultHeader};
        }
    }

    public void cancelRequests(Context context) {
        client.cancelRequests(context, true);
    }

    private Header createBasicAuthenticationHeader(String username,
                                                   String password) {
        String credentials = String.format("%s:%s", username, password);
        String encoded = Base64.encodeToString(credentials.getBytes(),
                Base64.NO_WRAP);
        return new BasicHeader("Authorization", String.format("%s %s", "Basic", encoded));
    }

    public void post(Context context, String url, Model model,
                     AsyncHttpResponseHandler response) {
        post(url, null, model.getData().toString(), response);
    }

    private void post(String url, ArrayList<Header> headers, String body,
                      AsyncHttpResponseHandler responseHandler) {

        String type = "POST";
        final String reqUrlWithParams = getAbsoluteUrlwithParams(url);
        if (headers == null) {
            headers = new ArrayList<>();
        }

        StringEntity bodyEntity;
        try {
            headers.add(reqSigner.getSignatureHeader(type, reqUrlWithParams, body));
            bodyEntity = new StringEntity(body, "UTF-8");
        } catch (UnsupportedEncodingException | URISyntaxException | SignatureException e) {
            Logger.error("Error creating signature on POST");
            return;
        }

        Header[] fullHeaders = prepareHeaders(headers);

        client.post(App.getAppInstance(), reqUrlWithParams, fullHeaders,
                bodyEntity, "application/json", responseHandler);
    }

    private String getAbsoluteUrlwithParams(String url) {
        String reqUrl = getAbsoluteUrl(url);
        return prepareUrl(reqUrl);
    }

    private String prepareUrl(String url) {
        // Not all methods of AsynchHttpClient take a requestParams.
        // Sometimes we will need to put the api key and other data
        // directly in the URL.
        return url + "?" + getTimestampQuery() + "&" + getAccessKeyQuery();
    }


    private String getAppVersion() {
        return prefs.getString("platform_ver_build", "");
    }

    /**
     * Executes a post request and adds basic authentication headers to the
     * request.
     */
    public void postWithAuthentication(String url, String username, String password,
                                       Model model, AsyncHttpResponseHandler responseHandler) {

        Header[] headers = {createBasicAuthenticationHeader(username, password)};
        String body = null;
        if (model != null) {
            body = model.getData().toString();
        }

        post(url, new ArrayList<>(Arrays.asList(headers)),
                body, responseHandler);
    }

    public void getImage(String imageUrl, BinaryHttpResponseHandler handler) {
        if (imageUrl.startsWith("/")) {
            imageUrl = safePathJoin(baseUrl, imageUrl);
        }
        client.get(imageUrl, handler);
    }

    public void postWithAuthentication(String url, String username, String password,
                                       AsyncHttpResponseHandler responseHandler) {

        postWithAuthentication(url, username, password, null, responseHandler);
    }

    // This overloading of the postWithAuthentication method takes a bitmap, and
    // posts it as an PNG HTTP Entity.
    public void postWithAuthentication(String url, Bitmap bm,
                                       String username, String password,
                                       JsonHttpResponseHandler responseHandler, int timeout) {

        String completeUrl = getAbsoluteUrl(url);
        completeUrl = prepareUrl(completeUrl);

        // Content type also needs to be pinned down in the Bitmap.compress
        // call, which is why I haven't exposed it as a parameter.
        String contentType = "image/jpeg";

        // We need to coerce the bitmap into a ByteArrayEntity so that we can
        // post it.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(CompressFormat.JPEG, 55, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayEntity bae = new ByteArrayEntity(bitmapdata);

        // No signature for http client which takes a BitmapEntity and a headers
        // array, so creating a one-off client for this purpose
        AsyncHttpClient authenticatedClient = createAutheniticatedHttpClient(
                username, password);

        // Add the signature based on the base64 encoded representation of the bitmap
        Header sig;
        try {
            sig = reqSigner.getSignatureHeader("POST", completeUrl, bitmapdata);
        } catch (URISyntaxException | SignatureException e) {
            Logger.error("Error creating signature on POST");
            return;
        }
        authenticatedClient.addHeader(sig.getName(), sig.getValue());

        authenticatedClient.setTimeout(timeout);
        authenticatedClient.post(App.getAppInstance(), completeUrl, bae, contentType,
                responseHandler);
    }

    private AsyncHttpClient createAutheniticatedHttpClient(String username,
                                                           String password) {
        AsyncHttpClient client = createHttpClient();
        Header header = createBasicAuthenticationHeader(username, password);
        client.addHeader(header.getName(), header.getValue());
        return client;
    }

    /**
     * Executes a put request and adds basic authentication headers to the
     * request.
     */
    public void putWithAuthentication(String url,
                                      String username, String password, int id, Model model,
                                      AsyncHttpResponseHandler response) {

        Header[] headers = {createBasicAuthenticationHeader(username, password)};
        String body = model.getData().toString();

        put(url, id, new ArrayList<>(Arrays.asList(headers)),
                body, response);
    }

    public void putWithAuthentication(String url,
                                      String username, String password, Model model,
                                      AsyncHttpResponseHandler response) {

        Header[] headers = {createBasicAuthenticationHeader(username, password)};
        String body = model.getData().toString();

        put(url, -1, new ArrayList<>(Arrays.asList(headers)),
                body, response);
    }

    private void put(String url, int id,
                     ArrayList<Header> headers,
                     String body,
                     AsyncHttpResponseHandler responseHandler) {
        String reqUrl = safePathJoin(getAbsoluteUrl(url), id == -1 ? "" : Integer.toString(id));
        String reqUrlWithParams = prepareUrl(reqUrl);
        if (headers == null) {
            headers = new ArrayList<>();
        }

        StringEntity bodyEntity;
        try {
            headers.add(reqSigner.getSignatureHeader("PUT", reqUrlWithParams, body));
            bodyEntity = new StringEntity(body, "UTF-8");
        } catch (UnsupportedEncodingException | URISyntaxException | SignatureException e) {
            Logger.error("Failure making PUT request", e);
            return;
        }

        Header[] fullHeaders = prepareHeaders(headers);
        client.put(App.getAppInstance(), reqUrlWithParams, fullHeaders,
                bodyEntity, "application/json", responseHandler);
    }

    public void put(String url, int id, Model model,
                    AsyncHttpResponseHandler response) {

        put(url, id, null, model.getData().toString(), response);
    }



}
