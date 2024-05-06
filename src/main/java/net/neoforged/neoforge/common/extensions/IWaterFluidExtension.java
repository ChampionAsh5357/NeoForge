package net.neoforged.neoforge.common.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidExtensions;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public interface IWaterFluidExtension extends IFluidExtension {

    Map<SoundAction, SoundEvent> SOUNDS = Map.of(
            SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL,
            SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY,
            SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH,
            SoundActions.CAULDRON_DRIP, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON
    );
    DripstoneDripInfo DRIP_INFO = new DripstoneDripInfo(PointedDripstoneBlock.WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK, ParticleTypes.DRIPPING_DRIPSTONE_WATER, Blocks.WATER_CAULDRON);

    private WaterFluid self() {
        return (WaterFluid) this;
    }

    @Override
    default String getDescriptionId() {
        return "block.minecraft.water";
    }

    @Override
    default float getFallDistanceModifier(Entity entity) {
        return 0F;
    }

    @Override
    default boolean canExtinguish() {
        return true;
    }

    @Override
    default boolean canConvertToSource() {
        return true;
    }

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
    default SoundEvent getSound(SoundAction action) {
        return SOUNDS.get(action);
    }

    @Override
    @Nullable
    default DripstoneDripInfo getDripInfo() {
        return DRIP_INFO;
    }

    @Override
    default boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
    }

    @Override
    default @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return canFluidLog ? IFluidExtension.super.getBlockPathType(state, level, pos, mob, true) : null;
    }

    @Override
    default void initializeClient(Consumer<IClientFluidExtensions> consumer) {
        consumer.accept(new IClientFluidExtensions() {
            private static final ResourceLocation UNDERWATER_LOCATION = new ResourceLocation("textures/misc/underwater.png"),
                    WATER_STILL = new ResourceLocation("block/water_still"),
                    WATER_FLOW = new ResourceLocation("block/water_flow"),
                    WATER_OVERLAY = new ResourceLocation("block/water_overlay");

            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                return UNDERWATER_LOCATION;
            }

            @Override
            public int getTintColor() {
                return 0xFF3F76E4;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return BiomeColors.getAverageWaterColor(getter, pos) | 0xFF000000;
            }
        });
    }
}
