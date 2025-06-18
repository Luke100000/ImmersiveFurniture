package net.conczin.immersive_furniture.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

    public static String capitalize(ResourceLocation location) {
        String fallback = capitalize(location.getPath());
        if (location.getPath().startsWith("block/")) {
            return Component.translatableWithFallback(
                    "block." + location.getNamespace() + ".." + location.getPath().substring(6),
                    fallback
            ).getString();
        } else if (location.getPath().startsWith("item/")) {
            return Component.translatableWithFallback(
                    "item." + location.getNamespace() + "." + location.getPath().substring(5),
                    fallback
            ).getString();
        }
        return fallback;
    }

    public static String capitalize(String location) {
        return StringUtils.capitalize(replaceUglyChars(location));
    }

    private static String replaceUglyChars(String location) {
        return location.replace(".", " ").replace("/", " ").replace("_", " ");
    }

    public static String beatifyPackID(String s) {
        String[] split = s.split("/");
        return replaceUglyChars(split[split.length - 1]).replace(".zip", "");
    }

    public static boolean search(String search, String value) {
        if (search.isEmpty()) return true;
        String lowerSearch = replaceUglyChars(search.toLowerCase());
        String lowerValue = replaceUglyChars(value.toLowerCase());
        return lowerValue.contains(lowerSearch);
    }
}
