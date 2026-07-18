package net.conczin.immersive_furniture.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class FurnitureSoundInstance extends AbstractTickableSoundInstance {
    public FurnitureSoundInstance(SoundEvent sound, float volume, float pitch, RandomSource random, Vec3 pos) {
        super(sound, SoundSource.BLOCKS, random);
        this.volume = volume;
        this.pitch = pitch;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.attenuation = Attenuation.LINEAR;
    }

    public void stopPlayback() {
        stop();
    }

    @Override
    public boolean canPlaySound() {
        return !isStopped();
    }

    @Override
    public void tick() {
    }
}
