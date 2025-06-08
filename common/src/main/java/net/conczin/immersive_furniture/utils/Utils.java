package net.conczin.immersive_furniture.utils;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

public class Utils {
    public static CompoundTag fromBytes(byte[] bytes) {
        try {
            return NbtIo.readCompressed(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBytes(CompoundTag tag) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteStream);

        try {
            NbtIo.writeCompressed(tag, dataOutput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteStream.toByteArray();
    }

    public static ListTag toFloatList(Vector3f vec) {
        ListTag listTag = new ListTag();
        listTag.add(FloatTag.valueOf(vec.x));
        listTag.add(FloatTag.valueOf(vec.y));
        listTag.add(FloatTag.valueOf(vec.z));
        return listTag;
    }

    public static Vector3f fromFloatList(ListTag from) {
        return new Vector3f(
                from.getFloat(0),
                from.getFloat(1),
                from.getFloat(2)
        );
    }

    public static String hashNbt(CompoundTag tag) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(baos);
        try {
            NbtIo.write(tag, dataOutput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] bytes = baos.toByteArray();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(bytes);
        return HexFormat.of().formatHex(hash);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String name, E defaultValue) {
        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }

    public static String capitalize(ResourceLocation location) {
        return StringUtils.capitalize(location.getPath().replace(".", " ").replace("/", " ").replace("_", " "));
    }

    public static ListTag toNbt(Set<String> stringSet) {
        ListTag listTag = new ListTag();
        for (String str : stringSet) {
            listTag.add(StringTag.valueOf(str));
        }
        return listTag;
    }

    public static Set<String> fromNbt(ListTag listTag) {
        Set<String> stringSet = new HashSet<>();
        for (Tag tag : listTag) {
            if (tag instanceof StringTag stringTag) {
                stringSet.add(stringTag.getAsString());
            }
        }
        return stringSet;
    }

    public static String beatifyPackID(String s) {
        String[] split = s.split("/");
        return split[split.length - 1].replace("_", " ").replace(".zip", "");
    }
}
