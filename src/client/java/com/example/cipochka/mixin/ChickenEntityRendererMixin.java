package com.example.cipochka.mixin;

import com.example.cipochka.CipochkaClient;
import net.minecraft.client.render.entity.ChickenEntityRenderer;
import net.minecraft.client.render.entity.state.ChickenEntityRenderState;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChickenEntityRenderer.class)
public abstract class ChickenEntityRendererMixin {

    @Unique
    private static final Identifier CIPOCHKA_ADULT_TEXTURE =
            Identifier.of(CipochkaClient.MOD_ID, "textures/entity/chicken/cipochka.png");

    @Unique
    private static final Identifier CIPOCHKA_BABY_TEXTURE =
            Identifier.of(CipochkaClient.MOD_ID, "textures/entity/chicken/cipochka_baby.png");

    @Unique
    private boolean cipochka$customChicken;

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void cipochka$updateRenderState(ChickenEntity entity, ChickenEntityRenderState state, float tickDelta, CallbackInfo ci) {
        this.cipochka$customChicken = isCipochkaName(entity.getCustomName());
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void cipochka$getTexture(ChickenEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (!this.cipochka$customChicken) {
            return;
        }

        cir.setReturnValue(state.baby ? CIPOCHKA_BABY_TEXTURE : CIPOCHKA_ADULT_TEXTURE);
    }

    @Unique
    private static boolean isCipochkaName(Text customName) {
        if (customName == null) {
            return false;
        }

        String name = customName.getString().trim();
        return name.equalsIgnoreCase("Cipochka") || name.equalsIgnoreCase("Цыпочка");
    }
}