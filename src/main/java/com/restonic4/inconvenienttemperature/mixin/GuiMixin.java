package com.restonic4.inconvenienttemperature.mixin;

import com.restonic4.inconvenienttemperature.InconvenientTemperature;
import com.restonic4.inconvenienttemperature.client.TemperatureBar;
import com.restonic4.inconvenienttemperature.client.ThermalClient;
import com.restonic4.inconvenienttemperature.core.ThermalEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    private static final ResourceLocation FREEZE_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final ResourceLocation HEAT_OVERLAY = InconvenientTemperature.id("textures/misc/heat_vignette.png");

    @Shadow
    protected abstract void renderTextureOverlay(GuiGraphics graphics, ResourceLocation texture, float alpha);

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void inconvenientTemperature$temperatureBar(GuiGraphics graphics, int left, CallbackInfo ci) {
        if (ThermalClient.showsTemperatureBar()) {
            TemperatureBar.render(graphics, left, ThermalClient.displayedTemperature());
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void inconvenientTemperature$hideLevel(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (ThermalClient.showsTemperatureBar()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCameraOverlays", at = @At("TAIL"))
    private void inconvenientTemperature$overlays(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        float temperature = ThermalClient.displayedTemperature();
        float intensity = ThermalEffects.visualIntensity(temperature);
        if (intensity <= 0f) return;
        renderTextureOverlay(graphics, temperature < 0f ? FREEZE_OVERLAY : HEAT_OVERLAY, intensity);
    }
}
