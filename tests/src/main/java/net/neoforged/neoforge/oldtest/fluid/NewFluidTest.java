/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidExtensions;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.DispenseFluidContainer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Mod(NewFluidTest.MODID)
public class NewFluidTest {
    public static final boolean ENABLE = false; // TODO fix
    public static final String MODID = "new_fluid_test";

    public static final ResourceLocation FLUID_STILL = new ResourceLocation("minecraft:block/brown_mushroom_block");
    public static final ResourceLocation FLUID_FLOWING = new ResourceLocation("minecraft:block/mushroom_stem");
    public static final ResourceLocation FLUID_OVERLAY = new ResourceLocation("minecraft:block/obsidian");

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MODID);

    private static BaseFlowingFluid.Properties makeProperties() {
        return new BaseFlowingFluid.Properties(test_fluid, test_fluid_flowing)
                .bucket(TEST_FLUID_BUCKET).block(test_fluid_block);
    }

    public static DeferredHolder<Fluid, FlowingFluid> test_fluid = FLUIDS.register("test_fluid", () -> new BaseFlowingFluid.Source(makeProperties()) {
        @Override
        public void initializeClient(Consumer<IClientFluidExtensions> consumer) {
            consumer.accept(new IClientFluidExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return FLUID_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return FLUID_FLOWING;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return FLUID_OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return 0x3F1080FF;
                }
            });
        }
    });
    public static DeferredHolder<Fluid, FlowingFluid> test_fluid_flowing = FLUIDS.register("test_fluid_flowing", () -> new BaseFlowingFluid.Flowing(makeProperties()) {
        @Override
        public void initializeClient(Consumer<IClientFluidExtensions> consumer) {
            consumer.accept(new IClientFluidExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return FLUID_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return FLUID_FLOWING;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return FLUID_OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return 0x3F1080FF;
                }
            });
        }
    });

    public static DeferredBlock<LiquidBlock> test_fluid_block = BLOCKS.register("test_fluid_block", () -> new LiquidBlock(test_fluid.value(), Properties.of().noCollission().strength(100.0F).noLootTable()));
    public static DeferredItem<Item> TEST_FLUID_BUCKET = ITEMS.register("test_fluid_bucket", () -> new BucketItem(test_fluid.value(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // WARNING: this doesn't allow "any fluid", only the fluid from this test mod!
    public static DeferredBlock<Block> fluidloggable_block = BLOCKS.register("fluidloggable_block", () -> new FluidloggableBlock(Properties.of().mapColor(MapColor.WOOD).noCollission().strength(100.0F).noLootTable()));
    public static DeferredItem<Item> FLUID_LOGGABLE_BLOCK_ITEM = ITEMS.register("fluidloggable_block", () -> new BlockItem(fluidloggable_block.get(), new Item.Properties()));

    public NewFluidTest(IEventBus modEventBus) {
        if (ENABLE) {
            modEventBus.addListener(this::loadComplete);

            BLOCKS.register(modEventBus);
            ITEMS.register(modEventBus);
            FLUIDS.register(modEventBus);
            modEventBus.addListener(this::addCreative);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FLUID_LOGGABLE_BLOCK_ITEM);
            event.accept(TEST_FLUID_BUCKET);
        }
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        // some sanity checks
        BlockState state = Fluids.WATER.defaultFluidState().createLegacyBlock();
        BlockState state2 = Fluids.WATER.getBlockForFluidState(null, null, Fluids.WATER.defaultFluidState());
        Validate.isTrue(state.getBlock() == Blocks.WATER && state2 == state);
        ItemStack stack = Fluids.WATER.getBucket(new FluidStack(Fluids.WATER, 1));
        Validate.isTrue(stack.getItem() == Fluids.WATER.getBucket());
        event.enqueueWork(() -> DispenserBlock.registerBehavior(TEST_FLUID_BUCKET.get(), DispenseFluidContainer.getInstance()));
    }

    // WARNING: this doesn't allow "any fluid", only the fluid from this test mod!
    private static class FluidloggableBlock extends Block implements SimpleWaterloggedBlock {
        public static final BooleanProperty FLUIDLOGGED = BooleanProperty.create("fluidlogged");

        public FluidloggableBlock(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(FLUIDLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FLUIDLOGGED);
        }

        @Override
        public boolean canPlaceLiquid(@Nullable Player player, BlockGetter worldIn, BlockPos pos, BlockState state, Fluid fluidIn) {
            return !state.getValue(FLUIDLOGGED) && fluidIn == test_fluid.get();
        }

        @Override
        public boolean placeLiquid(LevelAccessor worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
            if (canPlaceLiquid(null, worldIn, pos, state, fluidStateIn.getType())) {
                if (!worldIn.isClientSide()) {
                    worldIn.setBlock(pos, state.setValue(FLUIDLOGGED, true), 3);
                    worldIn.scheduleTick(pos, fluidStateIn.getType(), fluidStateIn.getType().getTickDelay(worldIn));
                }

                return true;
            } else {
                return false;
            }
        }

        @Override
        public ItemStack pickupBlock(@Nullable Player player, LevelAccessor worldIn, BlockPos pos, BlockState state) {
            if (state.getValue(FLUIDLOGGED)) {
                worldIn.setBlock(pos, state.setValue(FLUIDLOGGED, false), 3);
                return new ItemStack(TEST_FLUID_BUCKET.get());
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public FluidState getFluidState(BlockState state) {
            return state.getValue(FLUIDLOGGED) ? test_fluid.get().defaultFluidState() : Fluids.EMPTY.defaultFluidState();
        }
    }
}
