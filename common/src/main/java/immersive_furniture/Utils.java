package immersive_furniture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.joml.Vector3f;

import java.io.*;

public class Utils {
    public static CompoundTag fromBytes(byte[] bytes) {
        try {
            return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBytes(CompoundTag tag) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteStream);

        try {
            NbtIo.write(tag, dataOutput);
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
}
