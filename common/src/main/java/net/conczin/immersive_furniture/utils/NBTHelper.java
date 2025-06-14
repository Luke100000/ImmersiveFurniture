package net.conczin.immersive_furniture.utils;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for working with NBT data.
 * Provides getOrDefault methods for common data types.
 */
public class NBTHelper {

    /**
     * Gets a string value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The string value, or the default value if the key doesn't exist
     */
    public static String getString(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key) ? tag.getString(key) : defaultValue;
    }

    /**
     * Gets an int value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The int value, or the default value if the key doesn't exist
     */
    public static int getInt(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key) ? tag.getInt(key) : defaultValue;
    }

    /**
     * Gets a float value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The float value, or the default value if the key doesn't exist
     */
    public static float getFloat(CompoundTag tag, String key, float defaultValue) {
        return tag.contains(key) ? tag.getFloat(key) : defaultValue;
    }

    /**
     * Gets a double value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The double value, or the default value if the key doesn't exist
     */
    public static double getDouble(CompoundTag tag, String key, double defaultValue) {
        return tag.contains(key) ? tag.getDouble(key) : defaultValue;
    }

    /**
     * Gets a boolean value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The boolean value, or the default value if the key doesn't exist
     */
    public static boolean getBoolean(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key) ? tag.getBoolean(key) : defaultValue;
    }

    /**
     * Gets a byte value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The byte value, or the default value if the key doesn't exist
     */
    public static byte getByte(CompoundTag tag, String key, byte defaultValue) {
        return tag.contains(key) ? tag.getByte(key) : defaultValue;
    }

    /**
     * Gets a short value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The short value, or the default value if the key doesn't exist
     */
    public static short getShort(CompoundTag tag, String key, short defaultValue) {
        return tag.contains(key) ? tag.getShort(key) : defaultValue;
    }

    /**
     * Gets a long value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The long value, or the default value if the key doesn't exist
     */
    public static long getLong(CompoundTag tag, String key, long defaultValue) {
        return tag.contains(key) ? tag.getLong(key) : defaultValue;
    }

    /**
     * Gets a ResourceLocation from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The ResourceLocation, or the default value if the key doesn't exist
     */
    public static ResourceLocation getResourceLocation(CompoundTag tag, String key, ResourceLocation defaultValue) {
        return tag.contains(key) ? new ResourceLocation(tag.getString(key)) : defaultValue;
    }

    /**
     * Gets an enum value from the tag, or returns a default value if the key doesn't exist.
     *
     * @param tag          The CompoundTag to get the value from
     * @param enumClass    The enum class
     * @param key          The key to look up
     * @param defaultValue The default value to return if the key doesn't exist
     * @return The enum value, or the default value if the key doesn't exist
     */
    public static <E extends Enum<E>> E getEnum(CompoundTag tag, Class<E> enumClass, String key, E defaultValue) {
        try {
            return Enum.valueOf(enumClass, tag.getString(key).toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Converts a Vector3f to a ListTag of float values.
     *
     * @param vec The Vector3f to convert
     * @return A ListTag containing the x, y, and z components of the vector
     */
    public static ListTag getFloatList(Vector3f vec) {
        ListTag listTag = new ListTag();
        listTag.add(FloatTag.valueOf(vec.x));
        listTag.add(FloatTag.valueOf(vec.y));
        listTag.add(FloatTag.valueOf(vec.z));
        return listTag;
    }

    /**
     * Converts a ListTag of float values to a Vector3f.
     *
     * @param listTag The ListTag to convert
     * @return A Vector3f with components from the ListTag
     */
    public static Vector3f getVector3f(ListTag listTag) {
        return new Vector3f(
                listTag.getFloat(0),
                listTag.getFloat(1),
                listTag.getFloat(2)
        );
    }

    /**
     * Converts a Set of strings to a ListTag of string values.
     *
     * @param stringSet The Set of strings to convert
     * @return A ListTag containing the strings from the set
     */
    public static ListTag getStringList(Set<String> stringSet) {
        ListTag listTag = new ListTag();
        for (String str : stringSet) {
            listTag.add(StringTag.valueOf(str));
        }
        return listTag;
    }

    /**
     * Converts a ListTag of string values to a Set of strings.
     *
     * @param listTag The ListTag to convert
     * @return A Set containing the strings from the ListTag
     */
    public static Set<String> getStringSet(ListTag listTag) {
        Set<String> stringSet = new HashSet<>();
        for (Tag tag : listTag) {
            if (tag instanceof StringTag stringTag) {
                stringSet.add(stringTag.getAsString());
            }
        }
        return stringSet;
    }
}
