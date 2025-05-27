package immersive_furniture.client;

import immersive_furniture.Common;
import immersive_furniture.data.FurnitureData;
import immersive_furniture.data.api.API;
import immersive_furniture.data.api.responses.ContentResponse;
import immersive_furniture.data.api.responses.Response;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static immersive_furniture.data.api.API.request;

public class FurnitureDataManager {
    public static final Map<ResourceLocation, FurnitureData> DATA = new ConcurrentHashMap<>();
    public static final Set<ResourceLocation> REQUESTED_DATA = ConcurrentHashMap.newKeySet();

    private static File getFile(ResourceLocation id) {
        File file = new File("./immersive_furniture/" + id.getNamespace() + "/" + id.getPath() + ".nbt");

        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        return file;
    }

    private static File getLocalFile(FurnitureData data) {
        return getFile(getSafeLocalLocation(data));
    }

    private static void delete(File file) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public static String toSafeName(String input) {
        return input.replaceAll("[^a-z0-9_\\-.]", "_");
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
                .map(p -> new ResourceLocation("local", p.getName().replace(".nbt", "")))
                .toList();
    }

    public static ResourceLocation getSafeLocalLocation(FurnitureData data) {
        return new ResourceLocation("local", toSafeName(data.name.toLowerCase(Locale.ROOT)));
    }

    public static boolean localFileExists(FurnitureData data) {
        return getLocalFile(data).exists();
    }

    public static void deleteLocalFile(ResourceLocation selected) {
        delete(getLocalFile(DATA.get(selected)));
    }

    public static void saveLocalFile(FurnitureData data) {
        File cache = getLocalFile(data);
        try {
            NbtIo.writeCompressed(data.toTag(), cache);
            DATA.put(getSafeLocalLocation(data), data);
        } catch (IOException e) {
            Common.logger.error("Failed to save local file: {}", cache.getPath(), e);
        }
    }

    public static FurnitureData getData(ResourceLocation id) {
        if (!DATA.containsKey(id) && !REQUESTED_DATA.contains(id)) {
            REQUESTED_DATA.add(id);

            // Load if it exists
            File cache = getFile(id);
            if (cache.exists()) {
                try {
                    CompoundTag tag = NbtIo.read(cache);
                    if (tag == null) {
                        delete(cache);
                        Common.logger.error("Failed to read file: {}", cache);
                    } else {
                        FurnitureData data = new FurnitureData(tag);
                        DATA.put(id, data);
                        return data;
                    }
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
                        DataInput in = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(contentResponse.content().data())));
                        try {
                            FurnitureData data = new FurnitureData(NbtIo.read(in));
                            data.contentid = contentid;
                            data.author = contentResponse.content().username();
                            NbtIo.writeCompressed(data.toTag(), cache);
                            DATA.put(id, data);
                        } catch (Exception e) {
                            Common.logger.error("Failed to read content response: {}", contentResponse, e);
                        }
                    }
                });
            } else {
                // TODO: Request from server as part of data-packs
            }
        }
        return DATA.get(id);
    }
}
