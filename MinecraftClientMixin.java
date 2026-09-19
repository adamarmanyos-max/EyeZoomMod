package com.eyezoom.mixin;

import com.eyezoom.EyeZoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /** Key presses. Only drains while no screen is open, same as vanilla binds. */
    @Inject(method = "handleInputEvents", at = @At("RETURN"))
    private void eyezoom$handleInputEvents(CallbackInfo ci) {
        EyeZoom.handleInput((MinecraftClient) (Object) this);
    }

    /** Advance the smoothing every client tick, screen open or not. */
    @Inject(method = "tick", at = @At("RETURN"))
    private void eyezoom$tick(CallbackInfo ci) {
        EyeZoom.tick();
    }

    /**
     * Leaving a world with the zoom still active would strand the scaled
     * mouse sensitivity, and the next options.write() would persist it.
     */
    @Inject(method = "setWorld", at = @At("HEAD"))
    private void eyezoom$onWorldChange(@Nullable ClientWorld world, CallbackInfo ci) {
        EyeZoom.reset((MinecraftClient) (Object) this);
    }
}
