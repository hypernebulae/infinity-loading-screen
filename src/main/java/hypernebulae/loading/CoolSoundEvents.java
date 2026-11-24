package hypernebulae.loading;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.lang.reflect.*;

public class CoolSoundEvents {
    private static SoundEvent intro;

    public static SoundEvent awesomeIntro() {
        if (intro == null) {
            ResourceLocation id = Res.make("nothingtoseeheremovealong", "awesome_intro");
            try {
                for (Method m : SoundEvent.class.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == SoundEvent.class
                            && m.getParameterCount() == 1 && m.getParameterTypes()[0] == ResourceLocation.class) {
                        m.setAccessible(true);
                        intro = (SoundEvent) m.invoke(null, id);
                        break;
                    }
                }
                if (intro == null) {
                    Constructor<SoundEvent> c = SoundEvent.class.getDeclaredConstructor(ResourceLocation.class);
                    c.setAccessible(true);
                    intro = c.newInstance(id);
                }
            } catch (Throwable t) {
                throw new RuntimeException("Could not create intro sound event", t);
            }
        }
        return intro;
    }

    public static void init() {}
}