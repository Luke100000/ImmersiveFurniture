package net.conczin.immersive_furniture.network.s2c;

import net.conczin.immersive_furniture.InteractionManager;
import net.conczin.immersive_furniture.data.FurnitureData;
import net.conczin.immersive_furniture.network.ImmersivePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public record PoseOffsetMessage(
        BlockPos blockPos,
        Vector3f offset,
        Pose pose,
        float rotation,
        int entityId
) implements ImmersivePayload {
    public PoseOffsetMessage(BlockPos blockPos, FurnitureData.PoseOffset poseOffset, Entity entity) {
        this(blockPos, poseOffset.offset(), poseOffset.pose(), poseOffset.rotation(), entity.getId());
    }

    public PoseOffsetMessage(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()),
                buf.readEnum(Pose.class),
                buf.readFloat(),
                buf.readInt()
        );
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeFloat(offset.x());
        buf.writeFloat(offset.y());
        buf.writeFloat(offset.z());
        buf.writeEnum(pose);
        buf.writeFloat(rotation);
        buf.writeInt(entityId);
    }

    @Override
    public void handle(Player player) {
        FurnitureData.PoseOffset poseOffset = new FurnitureData.PoseOffset(offset, pose, rotation);
        Entity entity = player.level().getEntity(entityId);
        if (entity != null && entity != player) {
            InteractionManager.INSTANCE.addInteraction(entity, blockPos, poseOffset);
        }
    }
}
