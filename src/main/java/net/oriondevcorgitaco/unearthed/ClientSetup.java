package net.oriondevcorgitaco.unearthed;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.BlockItem;
import net.minecraft.world.GrassColors;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.oriondevcorgitaco.unearthed.block.BlockGeneratorHelper;
import net.oriondevcorgitaco.unearthed.block.BlockGeneratorReference;
import net.oriondevcorgitaco.unearthed.block.properties.ModBlockProperties;
import net.oriondevcorgitaco.unearthed.block.schema.BlockSchema;
import net.oriondevcorgitaco.unearthed.block.schema.Forms;
import net.oriondevcorgitaco.unearthed.core.UEBlocks;
import net.oriondevcorgitaco.unearthed.core.UEItems;
import net.oriondevcorgitaco.unearthed.util.ColorHelper;
import net.oriondevcorgitaco.unearthed.world.LichenColors;

@Mod.EventBusSubscriber(modid = Unearthed.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static void init(final FMLClientSetupEvent event) {
        for (BlockGeneratorHelper type : BlockGeneratorReference.ROCK_TYPES) {
            for (BlockGeneratorHelper.Entry entry : type.getEntries()) {
                BlockSchema.Form form = entry.getForm();
                if (form instanceof Forms.OreForm || form == Forms.GRASSY_REGOLITH || form == Forms.OVERGROWN_ROCK) {
                    RenderTypeLookup.setRenderLayer(entry.getBlock(), RenderType.cutoutMipped());
                }
            }
        }
        RenderTypeLookup.setRenderLayer(UEBlocks.LICHEN, RenderType.cutoutMipped());
    }

    @SubscribeEvent
    public static void onBlockColourHandleEvent(final ColorHandlerEvent.Block event) {
        for (BlockGeneratorHelper type : BlockGeneratorReference.ROCK_TYPES) {
            for (BlockGeneratorHelper.Entry entry : type.getEntries()) {
                BlockSchema.Form form = entry.getForm();
                if (form == Forms.GRASSY_REGOLITH || form == Forms.OVERGROWN_ROCK) {
                    event.getBlockColors().register((blockstate, reader, pos, i) -> {
                        return reader != null && pos != null ? BiomeColors.getAverageGrassColor(reader, pos) : GrassColors.get(0.5D, 1.0D);
                    }, entry.getBlock());
                }
            }
        }
        event.getBlockColors().register((blockstate, reader, pos, i) -> {
            return reader != null && pos != null ? LichenColors.shiftSaturation(reader.getBlockTint(pos, LichenColors.LICHEN_COLOR), pos, blockstate.getValue(ModBlockProperties.WET)) : LichenColors.getLichen();
        }, UEBlocks.LICHEN);
    }

    @SubscribeEvent
    public static void onItemColourHandlerEvent(final ColorHandlerEvent.Item event) {
        for (BlockGeneratorHelper type : BlockGeneratorReference.ROCK_TYPES) {
            for (BlockGeneratorHelper.Entry entry : type.getEntries()) {
                BlockSchema.Form form = entry.getForm();
                if (form == Forms.GRASSY_REGOLITH || form == Forms.OVERGROWN_ROCK) {
                    event.getItemColors().register((stack, color) -> {
                        BlockState blockstate = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
                        return event.getBlockColors().getColor(blockstate, null, null, color);
                    }, entry.getBlock());
                }
            }
        }
        event.getItemColors().register((stack, color) -> {
            return LichenColors.getLichen();
        }, UEItems.LICHEN);

    }
}