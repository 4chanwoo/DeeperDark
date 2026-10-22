package com.chanwoo.deeperdark.client;

import com.chanwoo.deeperdark.client.particle.VaseBreakParticle;
import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import com.chanwoo.deeperdark.core.DeeperDarkParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.render.RenderLayer;

public class DeeperDarkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), DeeperDarkBlocks.SCULK_CAPACITOR, DeeperDarkBlocks.SCULK_ORIFICE);
        ParticleFactoryRegistry.getInstance().register(DeeperDarkParticles.VASE_BREAK_PARTICLE, VaseBreakParticle.Factory::new);
    }
}
