package net.conczin.immersive_furniture.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.data.api.responses.ErrorResponse;
import net.conczin.immersive_furniture.data.api.responses.Response;
import net.conczin.immersive_furniture.data.api.responses.SuccessResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class API {
    private static final Gson gson = new Gson();
    private static final int TIMEOUT_MS = 10000;

    public enum HttpMethod {
        POST, GET, DELETE, PUT
    }

    public static Response request(HttpMethod httpMethod, String url) {
        return request(httpMethod, SuccessResponse.class, url, null, null);
    }

    public static Response request(HttpMethod httpMethod, Class<? extends Response> expectedAnswer, String url) {
        return request(httpMethod, expectedAnswer, url, null, null);
    }

    public static Response request(HttpMethod httpMethod, Class<? extends Response> expectedAnswer, String url, Map<String, String> queryParams) {
        return request(httpMethod, expectedAnswer, url, queryParams, null);
    }

    public static Response request(HttpMethod httpMethod, Class<? extends Response> expectedAnswer, String url, Map<String, String> queryParams, Map<String, ?> body) {
        try {
            String fullUrl = Config.getInstance().immersiveLibraryUrl + "/" + url;

            // Append query params
            if (queryParams != null) {
                fullUrl = queryParams.keySet().stream()
                        .map(key -> key + "=" + URLEncoder.encode(queryParams.get(key), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&", fullUrl + "?", ""));
            }

            HttpURLConnection con = (HttpURLConnection) (new URL(fullUrl)).openConnection();
            con.setConnectTimeout(TIMEOUT_MS);
            con.setReadTimeout(TIMEOUT_MS);

            // Set request method
            con.setRequestMethod(httpMethod.name());

            // Set request headers
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("Accept", "application/json");

            // Authenticate
            if (Auth.hasToken()) {
                con.setRequestProperty("Authorization", "Bearer " + Auth.getToken());
            }

            // Set request body
            if (body != null && !body.isEmpty()) {
                con.setDoOutput(true);
                String jsonBody = gson.toJson(body);
                con.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // Send the request and read the response
            int responseCode = con.getResponseCode();
            if (responseCode >= 400) {
                String error = con.getErrorStream() == null ? "" : IOUtils.toString(con.getErrorStream(), StandardCharsets.UTF_8);
                JsonObject object = gson.fromJson(error, JsonObject.class);
                boolean hasMessage = object != null && object.has("message");
                return new ErrorResponse(responseCode, hasMessage ? object.get("message").getAsString() : error);
            }

            // Parse answer
            String response;
            if ("gzip".equals(con.getContentEncoding())) {
                response = IOUtils.toString(new GZIPInputStream(con.getInputStream()), StandardCharsets.UTF_8);
            } else {
                response = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
            }

            return gson.fromJson(response, expectedAnswer);
        } catch (IOException e) {
            Common.logger.warn(e, e);
            return new ErrorResponse(-1, e.toString());
        } catch (Exception e) {
            Common.logger.error(e, e);
            return new ErrorResponse(-1, e.toString());
        }
    }
}
