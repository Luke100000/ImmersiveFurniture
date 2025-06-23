package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.Common;
import net.conczin.immersive_furniture.InteractionManager;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public record PoseOffsetMessage(BlockPos blockPos,
                                Vector3f offset,
                                Pose pose,
                                float rotation,
                                int entityId) implements ImmersivePayload {
    public static final CustomPacketPayload.Type<PoseOffsetMessage> TYPE = new CustomPacketPayload.Type<>(Common.locate("pose_offset_message"));
    public static final StreamCodec<FriendlyByteBuf, PoseOffsetMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PoseOffsetMessage::blockPos,
            StreamCodec.of(
                    (buf, vec) -> {
                        buf.writeFloat(vec.x);
                        buf.writeFloat(vec.y);
                        buf.writeFloat(vec.z);
                    },
                    (buf) -> new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat())
            ), PoseOffsetMessage::offset,
            Pose.STREAM_CODEC, PoseOffsetMessage::pose,
            ByteBufCodecs.FLOAT, PoseOffsetMessage::rotation,
            ByteBufCodecs.INT, PoseOffsetMessage::entityId,
            PoseOffsetMessage::new
    );

    public PoseOffsetMessage(BlockPos blockPos, FurnitureData.PoseOffset poseOffset, Entity entity) {
        this(blockPos, poseOffset.offset(), poseOffset.pose(), poseOffset.rotation(), entity.getId());
    }

    @Override
    public void handle(Player player) {
        FurnitureData.PoseOffset poseOffset = new FurnitureData.PoseOffset(offset, pose, rotation);
        Entity entity = player.level().getEntity(entityId);
        if (entity != null && entity != player) {
            InteractionManager.INSTANCE.addInteraction(entity, blockPos, poseOffset);
        }
    }

    @Override
    public Type<PoseOffsetMessage> type() {
        return TYPE;
    }
}
