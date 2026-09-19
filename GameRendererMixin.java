package com.eyezoom.mixin;

import com.eyezoom.EyeZoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The whole zoom is this one hook: divide the field of view that the game
 * was about to use. No second render pass, no framebuffer, no measurable
 * cost - which is what makes it viable on mobile hardware.
 *
 * getFov is private in 1.16.1 and returns a double.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void eyezoom$applyZoom(Camera camera, float tickDelta, boolean changingFov,
                                   CallbackInfoReturnable<Double> cir) {
        double divisor = EyeZoom.getFovDivisor(tickDelta);
        if (divisor > 1.001D) {
            cir.setReturnValue(cir.getReturnValue() / divisor);
        }
    }
}
