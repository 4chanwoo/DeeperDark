package com.chanwoo.deeperdark.client;

import com.chanwoo.deeperdark.client.particle.DeeperDarkParticleFactories;
import com.chanwoo.deeperdark.client.render.SculkOrificeBlockEntityRenderer;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public class DeeperDarkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DeeperDarkConfig.load();
        ClientScheduler.init();
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), DeeperDarkBlocks.SCULK_CAPACITOR, DeeperDarkBlocks.SCULK_ORIFICE);
        BlockEntityRendererRegistry.register(DeeperDarkBlockEntities.SCULK_ORIFICE, SculkOrificeBlockEntityRenderer::new);
        DeeperDarkParticleFactories.register();
    }
}
