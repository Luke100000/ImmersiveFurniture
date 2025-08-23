package net.conczin.immersive_furniture;

import net.minecraft.sounds.SoundEvent;

public interface Sounds {
    SoundEvent ASSEMBLE = SoundEvent.createVariableRangeEvent(Common.locate("assemble"));

    static void registerSounds(Common.RegisterHelper<SoundEvent> helper) {
        helper.register(ASSEMBLE.getLocation(), ASSEMBLE);
    }
}
