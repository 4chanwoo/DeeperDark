package com.chanwoo.deeperdark.client;

import com.chanwoo.deeperdark.client.particle.DeeperDarkParticleFactories;
import com.chanwoo.deeperdark.client.render.SculkOrificeBlockEntityRenderer;
import com.chanwoo.deeperdark.common.entity.SculkCapacitorBlockEntity;
import com.chanwoo.deeperdark.common.item.SculkCapacitorBlockItem;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import static com.chanwoo.deeperdark.core.DeeperDarkMod.MOD_ID;

public class DeeperDarkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DeeperDarkConfig.load();
        ClientScheduler.init();
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), DeeperDarkBlocks.SCULK_CAPACITOR, DeeperDarkBlocks.SCULK_ORIFICE);
        BlockEntityRendererRegistry.register(DeeperDarkBlockEntities.SCULK_ORIFICE, SculkOrificeBlockEntityRenderer::new);
        DeeperDarkParticleFactories.register();
        // Lets the capacitor item show how full it is, matching the placed block.
        FabricModelPredicateProviderRegistry.register(DeeperDarkBlocks.SCULK_CAPACITOR.asItem(), new Identifier(MOD_ID, "charge"),
                (stack, world, entity, seed) -> (float) SculkCapacitorBlockItem.getCharges(stack) / SculkCapacitorBlockEntity.MAX_CHARGES);
    }
}
