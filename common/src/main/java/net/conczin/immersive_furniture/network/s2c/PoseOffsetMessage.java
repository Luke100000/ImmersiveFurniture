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

public class PoseOffsetMessage implements ImmersivePayload {
    private final BlockPos blockPos;
    private final Vector3f offset;
    private final Pose pose;
    private final float rotation;
    private final int entityId;

    public PoseOffsetMessage(BlockPos blockPos, FurnitureData.PoseOffset poseOffset, Entity entity) {
        this.blockPos = blockPos;
        this.offset = poseOffset.offset();
        this.pose = poseOffset.pose();
        this.rotation = poseOffset.rotation();
        this.entityId = entity.getId();
    }

    public PoseOffsetMessage(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
        this.offset = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.pose = buf.readEnum(Pose.class);
        this.rotation = buf.readFloat();
        this.entityId = buf.readInt();
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
