package net.oriondevcorgitaco.unearthed.datagen;


import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.oriondevcorgitaco.unearthed.Unearthed;
import net.oriondevcorgitaco.unearthed.block.BlockGeneratorHelper;
import net.oriondevcorgitaco.unearthed.block.BlockGeneratorReference;
import net.oriondevcorgitaco.unearthed.block.schema.BlockSchema;
import net.oriondevcorgitaco.unearthed.block.schema.Forms;
import net.oriondevcorgitaco.unearthed.core.UEBlocks;
import net.oriondevcorgitaco.unearthed.core.UEItems;

import java.util.List;

public class Items extends ItemModelProvider {
    public Items(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, Unearthed.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<BlockSchema.Form> blockModelForms = Lists.newArrayList(Forms.BLOCK, Forms.SIDETOP_BLOCK, Forms.SLAB, Forms.SIDETOP_SLAB, Forms.STAIRS, Forms.SIDETOP_STAIRS, Forms.REGOLITH, Forms.AXISBLOCK, Forms.PRESSURE_PLATE);

        for (BlockGeneratorHelper type : BlockGeneratorReference.ROCK_TYPES) {
            for (BlockGeneratorHelper.Entry entry : type.getEntries()) {
                BlockSchema.Form form = entry.getForm();

                if (form == Forms.BUTTON || form == Forms.WALLS) {
                    blockInventoryModel(entry.getBlock());
                } else if (form == Forms.BEAM) {
                    String name = entry.getBlock().getRegistryName().getPath();
                    getBuilder(name).parent(new ModelFile.UncheckedModelFile(modLoc(ModelProvider.BLOCK_FOLDER + "/" + name + "_y")));
                } else {
                    blockItemModel(entry.getBlock());
                }
            }
        }
        simpleItem(UEItems.REGOLITH);
        simpleItem(UEItems.GOLD_ORE);
        simpleItem(UEItems.IRON_ORE);
        blockItemModel(UEBlocks.PYROXENE);
        blockItemModel(UEBlocks.LIGNITE_BRIQUETTES);

        simpleItem(UEBlocks.LICHEN, modLoc("block/lichen"));
    }

    private void simpleItem(IItemProvider provider) {
        String name = provider.asItem().getRegistryName().getPath();
        generated(name, modLoc("item/" + name));
    }

    private void simpleItem(IItemProvider provider, ResourceLocation texture) {
        String name = provider.asItem().getRegistryName().getPath();
        generated(name, texture);
    }

    private void generated(String path, ResourceLocation texture) {
        getBuilder(path).parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated"))).texture("layer0", texture);
    }

    private void blockItemModel(Block block) {
        String name = block.getRegistryName().getPath();
        getBuilder(name).parent(new ModelFile.UncheckedModelFile(modLoc(ModelProvider.BLOCK_FOLDER + "/" + name)));
    }

    private void blockInventoryModel(Block block) {
        String name = block.getRegistryName().getPath();
        getBuilder(name).parent(new ModelFile.UncheckedModelFile(modLoc(ModelProvider.BLOCK_FOLDER + "/" + name + "_inventory")));
    }

    @Override
    public String getName() {
        return "Unearthed Item Models";
    }
}
