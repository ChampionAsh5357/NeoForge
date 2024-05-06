package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidExtensions;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public interface ILavaFluidExtension extends IFluidExtension {

    Map<SoundAction, SoundEvent> SOUNDS = Map.of(
            SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA,
            SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA,
            SoundActions.CAULDRON_DRIP, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON
    );
    DripstoneDripInfo DRIP_INFO = new DripstoneDripInfo(PointedDripstoneBlock.LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK, ParticleTypes.DRIPPING_DRIPSTONE_LAVA, Blocks.LAVA_CAULDRON);


    private LavaFluid self() {
        return (LavaFluid) this;
    }

    @Override
    default String getDescriptionId() {
        return "block.minecraft.lava";
    }

    @Override
    default boolean canSwim(Entity entity) {
        return false;
    }

    @Override
    default boolean canDrownIn(LivingEntity entity) {
        return false;
    }

    @Override
    default @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return PathType.LAVA;
    }

    @Override
    @Nullable
    default PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
        return null;
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
    default int getLightLevel() {
        return 15;
    }

    @Override
    default int getDensity() {
        return 3000;
    }

    @Override
    default int getViscosity() {
        return 6000;
    }

    @Override
    default int getTemperature() {
        return 1300;
    }

    @Override
    default boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return level.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
    }

    @Override
    default double motionScale(Entity entity) {
        return entity.level().dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
    }

    @Override
    default void setItemMovement(ItemEntity entity) {
        Vec3 vec3 = entity.getDeltaMovement();
        entity.setDeltaMovement(vec3.x * (double) 0.95F, vec3.y + (double) (vec3.y < (double) 0.06F ? 5.0E-4F : 0.0F), vec3.z * (double) 0.95F);
    }

    @Override
    default void initializeClient(Consumer<IClientFluidExtensions> consumer) {
        consumer.accept(new IClientFluidExtensions() {
            private static final ResourceLocation LAVA_STILL = new ResourceLocation("block/lava_still"),
                    LAVA_FLOW = new ResourceLocation("block/lava_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return LAVA_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LAVA_FLOW;
            }
        });
    }
}
