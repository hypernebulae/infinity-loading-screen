package hypernebulae.loading;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class CoolSoundEvents {
    public static final SoundEvent AWESOME_INTRO = register("nothingtoseeheremovealong:awesome_intro");

    private static SoundEvent register(final String id) {
        Identifier sound = Identifier.parse(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, sound, SoundEvent.createVariableRangeEvent(sound));
    }

    public static void init() {}
}