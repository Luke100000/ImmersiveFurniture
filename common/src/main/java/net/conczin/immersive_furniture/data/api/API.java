package net.conczin.immersive_furniture.data.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class API {
    private static final Gson gson = new GsonBuilder().create();

    public enum HttpMethod {
        POST, GET, DELETE, PUT
    }

    public static Response request(HttpMethod httpMethod, String url) {
        return request(httpMethod, SuccessResponse::new, url, null, null);
    }

    public static <T extends Response> Response request(HttpMethod httpMethod, Function<JsonObject, T> decoder, String url) {
        return request(httpMethod, decoder, url, null, null);
    }

    public static <T extends Response> Response request(HttpMethod httpMethod, Function<JsonObject, T> decoder, String url, Map<String, String> queryParams) {
        return request(httpMethod, decoder, url, queryParams, null);
    }

    public static <T extends Response> Response request(HttpMethod httpMethod, Function<JsonObject, T> decoder, String url, Map<String, String> queryParams, Map<String, Object> body) {
        try {
            String fullUrl = Config.getInstance().immersiveLibraryUrl + (url.contains("v2") ? "/" : "/v1/") + url;

            // Append query params
            if (queryParams != null) {
                fullUrl = queryParams.keySet().stream()
                        .map(key -> key + "=" + URLEncoder.encode(queryParams.get(key), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&", fullUrl + "?", ""));
            }

            HttpURLConnection con = (HttpURLConnection) (new URL(fullUrl)).openConnection();

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
                Gson gson = new Gson();
                String jsonBody = gson.toJson(body);
                con.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // Send the request and read the response
            if (con.getErrorStream() != null) {
                int responseCode = con.getResponseCode();
                String error = IOUtils.toString(con.getErrorStream(), StandardCharsets.UTF_8);
                JsonObject object = gson.fromJson(error, JsonObject.class);
                return new ErrorResponse(responseCode, object.get("message").getAsString());
            }

            // Parse answer
            String response;
            if ("gzip".equals(con.getContentEncoding())) {
                response = IOUtils.toString(new GZIPInputStream(con.getInputStream()), StandardCharsets.UTF_8);
            } else {
                response = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
            }

            JsonObject object = gson.fromJson(response, JsonObject.class);
            return decoder.apply(object);
        } catch (IOException e) {
            Common.logger.warn(e, e);
            return new ErrorResponse(-1, e.toString());
        } catch (Exception e) {
            Common.logger.error(e, e);
            return new ErrorResponse(-1, e.toString());
        }
    }
}
