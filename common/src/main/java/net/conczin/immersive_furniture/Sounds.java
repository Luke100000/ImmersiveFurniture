package net.conczin.immersive_furniture;

import net.minecraft.sounds.SoundEvent;

public interface Sounds {
    SoundEvent REPAIR = SoundEvent.createVariableRangeEvent(Common.locate("repair"));

    static void registerSounds(Common.RegisterHelper<SoundEvent> helper) {
        helper.register(REPAIR.getLocation(), REPAIR);
    }
}
