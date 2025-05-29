package immersive_furniture.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFurnitureRegistry {
    public static Map<Integer, String> idToHash = new ConcurrentHashMap<>();
    public static Map<String, Integer> hashToId = new ConcurrentHashMap<>();

    public static String resolve(int identifier) {
        return idToHash.get(identifier);
    }

    public static int resolve(String hash) {
        return hashToId.getOrDefault(hash, 0);
    }
}
