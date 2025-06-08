package net.conczin.immersive_furniture.entity;

import net.conczin.immersive_furniture.Entities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SittingEntity extends Entity {
    private Vec3 dismountPosition;

    public SittingEntity(EntityType<SittingEntity> type, Level level) {
        super(type, level);
    }

    public SittingEntity(Level level, Vec3 pos, Vec3 dismountPosition) {
        super(Entities.SITTING.get(), level);

        setPos(pos.x, pos.y, pos.z);

        this.dismountPosition = dismountPosition;

        noPhysics = true;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return dismountPosition == null ? super.getDismountLocationForPassenger(passenger) : dismountPosition;
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
    public double getPassengersRidingOffset() {
        return -0.25f;
    }

    @Override
    protected void defineSynchedData() {

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
