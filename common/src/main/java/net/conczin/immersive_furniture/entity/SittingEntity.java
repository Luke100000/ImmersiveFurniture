package net.conczin.immersive_furniture.entity;

import net.conczin.immersive_furniture.block.BaseFurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SittingEntity extends Entity {
    private Vec3 dismountPosition;
    private BlockPos blockPos;
    private Vector3i size;
    private Direction direction;

    public SittingEntity(EntityType<SittingEntity> type, Level level) {
        super(type, level);
    }

    public SittingEntity(Level level, Vec3 pos, BlockPos blockPos, Vector3i size, Direction direction, Vec3 dismountPosition) {
        super(Entities.SITTING, level);

        setPos(pos.x, pos.y, pos.z);

        this.dismountPosition = dismountPosition;
        this.blockPos = blockPos;
        this.size = size;
        this.direction = direction;

        noPhysics = true;
    }

    public Vec3 tryDismountAt(LivingEntity passenger, BlockPos targetPos, int height) {
        BlockPos.MutableBlockPos pos = targetPos.mutable();
        double maxY = pos.getY() + height;

        // Try every pose
        for (Pose pose : passenger.getDismountPoses()) {
            pos.set(targetPos);

            // And move up until we find a valid floor
            while (pos.getY() < maxY) {
                double floorHeight = this.level().getBlockFloorHeight(pos);
                if (pos.getY() + floorHeight > maxY) break;

                if (DismountHelper.isBlockFloorValid(floorHeight)) {
                    AABB bounds = passenger.getLocalBoundsForPose(pose);
                    Vec3 candidate = new Vec3(pos.getX() + 0.5, pos.getY() + floorHeight, pos.getZ() + 0.5);
                    if (DismountHelper.canDismountTo(this.level(), passenger, bounds.move(candidate))) {
                        passenger.setPose(pose);
                        return candidate;
                    }
                }
                pos.move(Direction.UP);
            }
        }

        return null;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (blockPos == null || size == null || dismountPosition == null || direction == null) {
            return super.getDismountLocationForPassenger(passenger);
        }

        // Prefer a position close to the view direction of the passenger
        double radius = (Math.abs(size.x) + Math.abs(size.z)) / 2.0f;
        Vec3 center = new Vec3(
                getX() - Math.sin(Math.toRadians(passenger.getYRot())) * radius,
                getY(),
                getZ() + Math.cos(Math.toRadians(passenger.getYRot())) * radius
        );

        // Generate all block positions at this Y level
        List<BlockPos> candidates = new ArrayList<>();
        int y = (int) Math.floor(center.y);
        for (int x = -1; x <= size.x(); x++) {
            candidates.add(BaseFurnitureBlock.getProxyPosition(blockPos, direction, x, 0, -1));
            candidates.add(BaseFurnitureBlock.getProxyPosition(blockPos, direction, x, 0, size.z()));
        }
        for (int z = -1; z <= size.z(); z++) {
            candidates.add(BaseFurnitureBlock.getProxyPosition(blockPos, direction, -1, 0, z));
            candidates.add(BaseFurnitureBlock.getProxyPosition(blockPos, direction, size.x(), 0, z));
        }

        // Sort by horizontal distance to center
        candidates.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(center.x, center.y, center.z)));

        // Try to dismount at each position
        for (BlockPos pos : candidates) {
            Vec3 result = tryDismountAt(passenger, pos, size.y);
            if (result != null) return result;
        }

        return dismountPosition;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && (!isVehicle() || dismountPosition == null) && !isRemoved()) {
            discard();
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        super.onPassengerTurned(passenger);

        clampRotation(passenger);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);

        clampRotation(passenger);
    }

    public void clampRotation(Entity passenger) {
        float delta = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float clampedDelta = Mth.clamp(delta, -105.0f, 105.0f);
        passenger.yRotO += clampedDelta - delta;
        passenger.setYBodyRot(this.getYRot());
        passenger.setYRot(passenger.getYRot() + clampedDelta - delta);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return super.getPassengerRidingPosition(entity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
