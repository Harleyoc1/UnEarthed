package net.oriondevcorgitaco.unearthed.world.feature.stonegenerators;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.oriondevcorgitaco.unearthed.UnearthedConfig;
import net.oriondevcorgitaco.unearthed.core.UETags;
import net.oriondevcorgitaco.unearthed.datagen.type.IOreType;
import net.oriondevcorgitaco.unearthed.datagen.type.VanillaOreTypes;
import net.oriondevcorgitaco.unearthed.util.noise.FastNoiseLite;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.data.Cell;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.gen.AutomataRunner;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.data.State;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.data.Type;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.gen.NoiseHandler;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.gen.StratAutomata;
import net.oriondevcorgitaco.unearthed.world.feature.stonegenerators.gen.StratAutomata.StrataState;

import java.util.Random;

public class NewGenerator extends Feature<NoFeatureConfig> {
    public NewGenerator(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    private boolean isSeedSet = false;
    private static FastNoiseLite strataHeight;
    private boolean replaceCobble = false;
    private boolean replaceOres = false;
    private UnearthedConfig.DirtReplacement dirtReplacement = UnearthedConfig.DirtReplacement.HILLS;
    private boolean isAmplified = false;
    static BlockState air = Blocks.AIR.defaultBlockState();
    static BlockState GLASS = Blocks.GLASS.defaultBlockState();
    static BlockState DIAMOND = Blocks.DIAMOND_BLOCK.defaultBlockState();
    static BlockState EMERALD = Blocks.EMERALD_BLOCK.defaultBlockState();
    static BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    static BlockState IRON = Blocks.IRON_BLOCK.defaultBlockState();
    static BlockState NETHERITE = Blocks.NETHERITE_BLOCK.defaultBlockState();

    @Override
    public boolean place(ISeedReader world, ChunkGenerator generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        if (!isSeedSet) {
            setSeed(world.getSeed());
            NoiseHandler.setSeed(world.getSeed());
        }
//        boolean isAir = Math.abs(pos.getX() >> 4) % 5 < 2;
        isAmplified = generator instanceof NoiseChunkGenerator && ((NoiseChunkGenerator) generator).stable(world.getSeed(), DimensionSettings.AMPLIFIED);
        boolean debugMode = UnearthedConfig.debug.get();
        replaceCobble = UnearthedConfig.replaceCobble.get();
        replaceOres = UnearthedConfig.replaceOres.get();
        dirtReplacement = UnearthedConfig.dirtReplacement.get();
        NoiseHandler noiseHandler = new NoiseHandler(world, pos);
        AutomataRunner runner = new AutomataRunner(world, pos, noiseHandler);
        if (debugMode) {
            setDebugResults(world, pos, runner, noiseHandler);
        } else {
            setResults(world, pos, runner, noiseHandler);
        }
        return true;
    }

    private void setSeed(long seed) {
        if (strataHeight == null) {
            strataHeight = new FastNoiseLite((int) seed + 4568665);
            strataHeight.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2S);
            strataHeight.SetFrequency(0.001f);
            strataHeight.SetFractalOctaves(3);
        }
    }

    private int stratumLevelSize = 35;
    private static IOreType[] ores = VanillaOreTypes.values();

    private void setResults(ISeedReader world, BlockPos pos, AutomataRunner runner, NoiseHandler noiseHandler) {
        boolean alwaysReplaceDirt = isAmplified || dirtReplacement == UnearthedConfig.DirtReplacement.ALL;

        State[][][] result = runner.getResults();
        int[][] maxHeights = runner.getMaxHeights();
        int strataLevels = runner.getMaxHeight() / stratumLevelSize + 1;
        StrataState[][][] strataResults = new StrataState[strataLevels][][];
        StratAutomata stratAutomata = new StratAutomata(world.getSeed(), pos, noiseHandler);
        for (int i = 0; i < strataLevels; i++) {
            stratAutomata.set(i, 8, 3, 3);
            strataResults[i] = stratAutomata.fillInStates().propagate().nextStage().propagate().simpleSelect().nextStage().propagate().getResults();
        }
        IChunk chunk = world.getChunk(pos);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int posX = pos.getX() + x;
                int posZ = pos.getZ() + z;
                mutable.set(posX, 0, posZ);
                int stratumHeight = -1;
                int stratumDepth = 0;
                State stratumState = null;
                int topY = maxHeights[x][z];
                for (int y = 0; y < topY; y++) {
                    if (y % stratumLevelSize == 0) {
                        strataHeight.SetSeed((int) world.getSeed() + (y / stratumLevelSize) * 199327);
                        StrataState stratum = strataResults[y / stratumLevelSize][x][z];
                        if (stratum == null) {
                            stratumHeight = -1;
                        } else {
                            stratumHeight = y + (int) (Math.sqrt(strataHeight.GetNoise(posX, posZ) / 2 + 0.5) * stratumLevelSize);
                            stratumDepth = stratum.getThickness();
                            stratumState = stratum.getState();
                        }
                    }
                    boolean isInStrata = stratumHeight != -1 && stratumHeight <= y && y < stratumHeight + stratumDepth;
                    State state = result[x][z][y];
                    if (isInStrata && (state.getType() != Type.BATHOLITH || state.getType() != Type.OROGEN)) {
                        state = stratumState;
                    }
                    BlockState original = chunk.getBlockState(mutable);
                    boolean replaceDirt = alwaysReplaceDirt || dirtReplacement == UnearthedConfig.DirtReplacement.HILLS && isBiomeHilly(noiseHandler.getBiome(x, z));
                    BlockState replaced = replaceBlock(original, state, replaceDirt);
                    if (original != replaced && replaced != null) {
                        chunk.getSections()[y >> 4].setBlockState(x, y & 15, z, replaced, false);
//                        chunk.setBlockState(mutable, replaced, false);
                    }  //                        chunk.setBlockState(mutable, original, false);

                    mutable.move(Direction.UP);
                }
            }
        }
    }

    private boolean isBiomeHilly(Biome biome) {
        String biomeName = biome.getRegistryName().getPath();
        return biome.getBiomeCategory() == Biome.Category.EXTREME_HILLS || biomeName.contains("hill") || biomeName.contains("plateau") || biomeName.contains("mountain");
    }

    private BlockState replaceBlock(BlockState original, State state, boolean replaceDirt) {
//        return state.getDefaultState();
        if (UnearthedConfig.isReplaceableStone(original.getBlockState())) {
            return state.getCell().getDefaultState();
        } else if (original.is(Blocks.AIR)) {
            return original;
        } else {
            Cell cell = state.getCell();
            if (state.getType() == Type.TERTIARY) {
                if (original.is(UETags.Blocks.REPLACE_DIRT) || original.is(UETags.Blocks.REPLACE_GRASS)) {
                    return cell.getDefaultState();
                }
                for (IOreType oreType : ores) {
                    if (original.is(oreType.getBlock())) {
                        return cell.getDefaultState();
                    }
                }
            } else {
                if (replaceCobble && original.is(Blocks.COBBLESTONE)) {
                    return cell.getCobbleReplacement();
                }
                if (replaceDirt) {
                    if (original.is(UETags.Blocks.REPLACE_DIRT)) {
                        return cell.getDirtReplacement();
                    } else if (original.is(UETags.Blocks.REPLACE_GRASS)) {
                        return cell.getGrassReplacement(original);
                    }
                }
                if (replaceOres && cell.replacesOre()) {
                    for (IOreType oreType : ores) {
                        if (original.is(oreType.getBlock())) {
                            BlockState ore = cell.getOre(oreType);
                            if (ore != null) {
                                return ore;
                            } else return original;
                        }
                    }
                }
            }
        }
        return original;
    }


    private void setDebugResults(ISeedReader world, BlockPos pos, AutomataRunner runner, NoiseHandler noiseHandler) {
        boolean alwaysReplaceDirt = isAmplified || dirtReplacement == UnearthedConfig.DirtReplacement.ALL;

        State[][][] result = runner.debugResults();
        int[][] maxHeights = runner.getMaxHeights();
        int strataLevels = runner.getMaxHeight() / stratumLevelSize + 1;
        StrataState[][][] strataResults = new StrataState[strataLevels][][];
        StratAutomata stratAutomata = new StratAutomata(world.getSeed(), pos, noiseHandler);
        for (int i = 0; i < strataLevels; i++) {
            stratAutomata.set(i, 8, 3, 3);
            strataResults[i] = stratAutomata.fillInStates().propagate().simpleSelect().nextStage().propagate().nextStage().propagate().getResults();
        }
        IChunk chunk = world.getChunk(pos);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int posX = pos.getX() + x;
                int posZ = pos.getZ() + z;
                mutable.set(posX, 0, posZ);

                int stratumHeight = -1;
                int stratumDepth = 0;
                State stratumState = null;
                int topY = maxHeights[x][z];
                for (int y = 0; y < topY; y++) {
                    if (y % stratumLevelSize == 0) {
                        strataHeight.SetSeed((int) world.getSeed() + (y / stratumLevelSize) * 199327);
                        StrataState stratum = null;
                        stratum = strataResults[y / stratumLevelSize][x][z];
                        if (stratum == null) {
                            stratumHeight = -1;
                        } else {
                            stratumHeight = y + (int) (Math.sqrt(strataHeight.GetNoise(posX, posZ) / 2 + 0.5) * stratumLevelSize);
                            stratumDepth = stratum.getThickness();
                            stratumState = stratum.getState();
                        }
                    }
                    boolean isInStrata = stratumHeight != -1 && stratumHeight <= y && y < stratumHeight + stratumDepth;
                    State state = result[x][z][y];
                    if (isInStrata && (state.getType() != Type.BATHOLITH || state.getType() != Type.OROGEN)) {
                        state = stratumState;
                    }
                    BlockState original = chunk.getBlockState(mutable);
                    BlockState replaced;
                    if (state.getType() == Type.PRIMARY) {
                        replaced = GLASS;
                    } else {
                        boolean replaceDirt = alwaysReplaceDirt || dirtReplacement == UnearthedConfig.DirtReplacement.HILLS && isBiomeHilly(noiseHandler.getBiome(x, z));

                        replaced = replaceBlock(original, state, replaceDirt);
                    }
                    if (original != replaced && replaced != null) {
                        chunk.setBlockState(mutable, replaced, false);
                    }
                    mutable.move(Direction.UP);
                }
            }
        }
    }
}
