package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.data.api.API;
import net.conczin.immersive_furniture.data.api.responses.ContentResponse;
import net.conczin.immersive_furniture.data.api.responses.Response;
import net.conczin.immersive_furniture.network.Network;
import net.conczin.immersive_furniture.network.c2s.FurnitureDataRequest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static net.conczin.immersive_furniture.data.api.API.request;

public class FurnitureDataManager {
    public static final Path gameRoot = Path.of("./immersive_furniture");
    public static Path worldRoot = Path.of("./immersive_furniture");

    public static final Map<ResourceLocation, FurnitureData> DATA = new ConcurrentHashMap<>();
    public static final Set<ResourceLocation> REQUESTED_DATA = ConcurrentHashMap.newKeySet();

    private static final Set<String> alreadySaved = new ConcurrentSkipListSet<>();

    public static void setWorldRoot(Path worldPath) {
        worldRoot = worldPath.resolve("immersive_furniture").normalize();
        alreadySaved.clear();
    }

    public static void setWorldRoot() {
        worldRoot = gameRoot;
        alreadySaved.clear();
    }

    private static File getFile(ResourceLocation id) {
        // Most files are stored in the game root
        String path = id.getNamespace() + "/" + id.getPath() + ".nbt";
        File oldFile = gameRoot.resolve(path).toFile();
        if (oldFile.exists()) return oldFile;

        // But the hashed files are stored in the world root to allow copying worlds
        File file = oldFile;
        if (id.getNamespace().equals("hash")) {
            file = worldRoot.resolve(path).toFile();
        }

        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        return file;
    }

    private static void delete(File file) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public static String toSafeName(String input) {
        String safe = input.replaceAll("[^a-z0-9_\\-.]", "_");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return safe + "_" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ResourceLocation> getLocalFiles() {
        File cache = new File("./immersive_furniture/local");
        File[] files = cache.listFiles();
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .filter(p -> p.getPath().endsWith(".nbt"))
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .map(p -> ResourceLocation.fromNamespaceAndPath("local", p.getName().replace(".nbt", "")))
                .toList();
    }

    public static ResourceLocation getSafeLocalLocation(FurnitureData data) {
        return ResourceLocation.fromNamespaceAndPath("local", toSafeName(data.name.toLowerCase(Locale.ROOT)));
    }

    public static boolean localFileExists(FurnitureData data) {
        return getFile(getSafeLocalLocation(data)).exists();
    }

    public static void deleteLocalFile(ResourceLocation location) {
        delete(getFile(location));
    }

    public static void saveLocalFile(FurnitureData data) {
        save(data, getSafeLocalLocation(data));
    }

    public static void save(FurnitureData data, ResourceLocation id) {
        File cache = getFile(id);
        try {
            NbtIo.writeCompressed(data.toTag(), cache.toPath());
            DATA.put(id, data);
        } catch (IOException e) {
            Common.logger.error("Failed to save local file: {}", cache.getPath(), e);
        }
    }

    /**
     * Saves the furniture data to the hash storage.
     */
    public static void saveHashData(FurnitureData data) {
        String hash = data.getHash();
        if (alreadySaved.contains(hash)) return;
        alreadySaved.add(hash);
        FurnitureDataManager.save(data, ResourceLocation.fromNamespaceAndPath("hash", data.getHash()));
    }

    /**
     * Fetches from cached or hash storage in situations where it's not fully clear whether the call comes from a server or client.
     */
    public static FurnitureData getData(String hash) {
        ResourceLocation cachedLocation = ResourceLocation.fromNamespaceAndPath("cache", hash);
        if (DATA.containsKey(cachedLocation)) {
            return DATA.get(cachedLocation);
        }
        return getHashData(hash);
    }

    /**
     * Client-sided access via cache, fetches from the server when not available.
     */
    public static FurnitureData getCachedData(String hash) {
        return getData(ResourceLocation.fromNamespaceAndPath("cache", hash));
    }

    /**
     * Server-sided access via hash storage.
     */
    public static FurnitureData getHashData(String hash) {
        return getData(ResourceLocation.fromNamespaceAndPath("hash", hash));
    }

    public static FurnitureData getData(ResourceLocation id) {
        if (!DATA.containsKey(id) && !REQUESTED_DATA.contains(id)) {
            REQUESTED_DATA.add(id);

            // Load if it exists
            File cache = getFile(id);
            if (cache.exists()) {
                try {
                    CompoundTag tag = NbtIo.readCompressed(cache.toPath(), NbtAccounter.unlimitedHeap());
                    FurnitureData data = new FurnitureData(tag);
                    DATA.put(id, data);
                } catch (IOException e) {
                    delete(cache);
                    Common.logger.error("Failed to read file: {}", cache, e);
                }
            }

            // Request it otherwise
            if (id.getNamespace().equals("library")) {
                int contentid, version;
                try {
                    String[] split = id.getPath().split("\\.");
                    contentid = Integer.parseInt(split[0]);
                    version = Integer.parseInt(split[1]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    Common.logger.error("Failed to parse content id and version from: {}", id, e);
                    return null;
                }

                // Download assets when versions mismatch
                CompletableFuture.runAsync(() -> {
                    Response response = request(API.HttpMethod.GET, ContentResponse::new, "content/furniture/" + contentid, Map.of("version", String.valueOf(version)));
                    if (response instanceof ContentResponse contentResponse) {
                        ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(contentResponse.content().data()));
                        try {
                            FurnitureData data = new FurnitureData(NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
                            data.contentid = contentid;
                            data.author = contentResponse.content().username();
                            NbtIo.writeCompressed(data.toTag(), getFile(id).toPath());
                            DATA.put(id, data);
                        } catch (Exception e) {
                            Common.logger.error("Failed to read content response: {}", contentResponse, e);
                        }
                    }
                });
            } else if (id.getNamespace().equals("cache")) {
                Network.sendToServer(new FurnitureDataRequest(id.getPath()));
            }
        }
        return DATA.get(id);
    }
}
