package com.eyezoom.mixin;

import com.eyezoom.EyeZoom;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registers our binds without Fabric API. Appending to keysAll gets them
 * ticked, saved in options.txt and listed in the vanilla Controls screen -
 * which is also where Amethyst users will map them onto touch buttons.
 */
@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow
    public KeyBinding[] keysAll;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void eyezoom$addKeyBindings(CallbackInfo ci) {
        List<KeyBinding> all = new ArrayList<>(Arrays.asList(this.keysAll));
        all.addAll(EyeZoom.KEYS);
        this.keysAll = all.toArray(new KeyBinding[0]);
    }
}
