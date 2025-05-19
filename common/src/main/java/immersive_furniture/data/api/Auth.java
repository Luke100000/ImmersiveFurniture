package immersive_furniture.data.api;

import com.google.gson.JsonObject;
import immersive_furniture.Common;
import immersive_furniture.config.Config;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Auth {
    static final SecureRandom random = new SecureRandom();

    static String currentToken;

    private static String newToken() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return sha256(new String(bytes));
    }

    private static @NotNull Path getTokenPath() {
        return Paths.get("./immersiveLibraryTokenFurniture");
    }

    public static String loadToken() {
        try {
            return Files.readString(getTokenPath());
        } catch (IOException e) {
            return null;
        }
    }

    public static String getToken() {
        if (currentToken == null) {
            currentToken = loadToken();
        }
        return currentToken;
    }

    public static boolean hasToken() {
        return getToken() != null;
    }

    public static void saveToken() {
        try {
            Files.writeString(Paths.get("./immersiveLibraryToken_v2"), currentToken);
        } catch (IOException e) {
            Common.logger.error(e);
        }
    }

    public static void clearToken() {
        //noinspection ResultOfMethodCallIgnored
        getTokenPath().toFile().delete();
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createDataState(String username, String token) {
        JsonObject json = new JsonObject();
        json.addProperty("username", Base64.getEncoder().encodeToString(username.getBytes()));
        json.addProperty("token", Base64.getEncoder().encodeToString(sha256(token).getBytes()));
        return Base64.getEncoder().encodeToString(json.toString().getBytes());
    }

    public static void authenticate(String username) {
        // The unique, private token used to authenticate once authorized
        currentToken = newToken();

        // Open the authorization URL in the user's default web browser
        String url = Config.getInstance().immersiveLibraryUrl + "/v1/login?state=" + createDataState(username, currentToken);
        Util.getPlatform().openUri(url);
    }
}