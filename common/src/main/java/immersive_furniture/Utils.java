package immersive_furniture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.joml.Vector3f;

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
}
