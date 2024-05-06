/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidExtensions;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A test case used to define and test fluid type integration into fluids.
 *
 * <ul>
 * <li>Checks that each fluid has a fluid type</li>
 * <li>Adds a new fluid with its type, source/flowing, block, and bucket</li>
 * <li>Sets properties to test out fluid logic</li>
 * <li>Overrides fluid rendering methods</li>
 * <li>Adds block color and render layer</li>
 * <li>Adds fluid interaction definitions</li>
 * <li>Adds the ability for the fluid to drip from Pointed Dripstone stalactites into a cauldron below</li>
 * </ul>
 */
@Mod(FluidTypeTest.ID)
public class FluidTypeTest {
    private static final boolean ENABLE = true;

    protected static final String ID = "fluid_type_test";
    private static Logger logger;

    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(TEST_FLUID, TEST_FLUID_FLOWING)
                .block(TEST_FLUID_BLOCK)
                .bucket(TEST_FLUID_BUCKET);
    }

    private static final TagKey<Fluid> TEST_FLUID_TAG = FluidTags.create(new ResourceLocation(ID, "test_fluid"));

    private interface TestFluid extends IFluidExtension {

        DripstoneDripInfo DRIP_INFO = new DripstoneDripInfo(0.25F, ParticleTypes.SCULK_SOUL, Blocks.POWDER_SNOW_CAULDRON);
        Map<SoundAction, SoundEvent> SOUNDS = Map.of(
                SoundActions.CAULDRON_DRIP, SoundEvents.END_PORTAL_SPAWN
        );

        @Override
        default boolean supportsBoating(Boat boat) {
            return true;
        }

        @Override
        default boolean canHydrate() {
            return true;
        }

        @Override
        @Nullable
        default DripstoneDripInfo getDripInfo() {
            return DRIP_INFO;
        }

        @Override
        default @Nullable SoundEvent getSound(SoundAction action) {
            return SOUNDS.get(action);
        }

        @Override
        default void initializeClient(Consumer<IClientFluidExtensions> consumer) {
            consumer.accept(new IClientFluidExtensions() {
                private static final ResourceLocation STILL = new ResourceLocation("block/water_still"),
                        FLOW = new ResourceLocation("block/water_flow"),
                        OVERLAY = new ResourceLocation("block/obsidian"),
                        VIEW_OVERLAY = new ResourceLocation("textures/block/obsidian.png");

                @Override
                public ResourceLocation getStillTexture() {
                    return STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return FLOW;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return OVERLAY;
                }

                @Override
                public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                    return VIEW_OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return 0xAF7FFFD4;
                }

                @Override
                public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    int color = this.getTintColor();
                    return new Vector3f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
                }

                @Override
                public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                    nearDistance = -8F;
                    farDistance = 24F;

                    if (farDistance > renderDistance) {
                        farDistance = renderDistance;
                        shape = FogShape.CYLINDER;
                    }

                    RenderSystem.setShaderFogStart(nearDistance);
                    RenderSystem.setShaderFogEnd(farDistance);
                    RenderSystem.setShaderFogShape(shape);
                }

                @Override
                public boolean renderFluid(FluidState fluidState, BlockAndTintGetter getter, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState) {
                    // Flip RGB to BGR *only* for fluid blocks rendered at Y 100
                    if (pos.getY() == 100) {
                        vertexConsumer = new VertexConsumerWrapper(vertexConsumer) {
                            @Override
                            public VertexConsumer color(int r, int g, int b, int a) {
                                return super.color(b, g, r, a);
                            }
                        };
                    }
                    // Replace vanilla fluid rendering
                    Minecraft.getInstance().getBlockRenderer().getLiquidBlockRenderer().tesselate(getter, pos, vertexConsumer, blockState, fluidState);
                    return true;
                }
            });
        }

        class Source extends BaseFlowingFluid.Source implements TestFluid {

            public Source(Properties properties) {
                super(properties);
            }
        }

        class Flowing extends BaseFlowingFluid.Flowing implements TestFluid {

            public Flowing(Properties properties) {
                super(properties);
            }
        }
    }

    private static final DeferredHolder<Fluid, FlowingFluid> TEST_FLUID = FLUIDS.register("test_fluid", () -> new TestFluid.Source(fluidProperties()));
    private static final DeferredHolder<Fluid, Fluid> TEST_FLUID_FLOWING = FLUIDS.register("test_fluid_flowing", () -> new TestFluid.Flowing(fluidProperties()));
    private static final DeferredBlock<LiquidBlock> TEST_FLUID_BLOCK = BLOCKS.register("test_fluid_block", () -> new LiquidBlock(TEST_FLUID.get(), BlockBehaviour.Properties.of().noCollission().strength(100.0F).noLootTable()));
    private static final DeferredItem<Item> TEST_FLUID_BUCKET = ITEMS.register("test_fluid_bucket", () -> new BucketItem(TEST_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public FluidTypeTest(IEventBus modEventBus) {
        if (ENABLE) {
            logger = LogManager.getLogger();
            NeoForgeMod.enableMilkFluid();

            FLUIDS.register(modEventBus);
            BLOCKS.register(modEventBus);
            ITEMS.register(modEventBus);

            modEventBus.addListener(this::commonSetup);
            modEventBus.addListener(this::addCreative);
            modEventBus.addListener(this::dataGen);

            if (FMLEnvironment.dist.isClient()) {
                new FluidTypeTestClient(modEventBus);
            }
        }
    }

    private void dataGen(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new FluidTagsProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(), ID, event.getExistingFileHelper()) {
            @Override
            protected void addTags(HolderLookup.Provider p_256366_) {
                this.tag(TEST_FLUID_TAG).add(TEST_FLUID.value(), TEST_FLUID_FLOWING.value());
            }
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.accept(TEST_FLUID_BUCKET);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Add Interactions for sources
        FluidInteractionRegistry.addInteraction(TEST_FLUID.value(), new FluidInteractionRegistry.InteractionInformation(FluidTags.LAVA, Blocks.GOLD_BLOCK.defaultBlockState()));
        FluidInteractionRegistry.addInteraction(TEST_FLUID_FLOWING.value(), new FluidInteractionRegistry.InteractionInformation(FluidTags.LAVA, Blocks.GOLD_BLOCK.defaultBlockState()));

        FluidInteractionRegistry.addInteraction(Fluids.WATER, new FluidInteractionRegistry.InteractionInformation(TEST_FLUID_TAG, Blocks.DIAMOND_BLOCK.defaultBlockState()));
        FluidInteractionRegistry.addInteraction(Fluids.FLOWING_WATER, new FluidInteractionRegistry.InteractionInformation(TEST_FLUID_TAG, Blocks.IRON_BLOCK.defaultBlockState()));
    }

    private static class FluidTypeTestClient {
        private FluidTypeTestClient(IEventBus modEventBus) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerBlockColors);
        }

        private void clientSetup(FMLClientSetupEvent event) {
            Stream.of(TEST_FLUID, TEST_FLUID_FLOWING).map(DeferredHolder::get)
                    .forEach(fluid -> ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent()));
        }

        private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
            event.register((state, getter, pos, index) -> {
                if (getter != null && pos != null) {
                    FluidState fluidState = getter.getFluidState(pos);
                    return IClientFluidExtensions.of(fluidState).getTintColor(fluidState, getter, pos);
                } else return 0xAF7FFFD4;
            }, TEST_FLUID_BLOCK.get());
        }
    }
}
