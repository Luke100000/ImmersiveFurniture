package immersive_furniture.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FurnitureRegistry {
    public static final FurnitureRegistry INSTANCE = new FurnitureRegistry();

    public Map<Integer, String> identifierToHash = new ConcurrentHashMap<>();
    public Map<String, Integer> hashToIdentifier = new ConcurrentHashMap<>();

    public static String resolve(int identifier) {
        return INSTANCE.identifierToHash.get(identifier);
    }

    public static int resolve(String hash) {
        return INSTANCE.hashToIdentifier.getOrDefault(hash, 0);
    }
}
