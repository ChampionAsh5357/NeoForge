package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

public interface IEmptyFluidExtension extends IFluidExtension {

    private EmptyFluid self() {
        return (EmptyFluid) this;
    }

    @Override
    default String getDescriptionId() {
        return "block.minecraft.air";
    }

    @Override
    default double motionScale(Entity entity) {
        return 1D;
    }

    @Override
    default boolean canPushEntity(Entity entity) {
        return false;
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
    default float getFallDistanceModifier(Entity entity) {
        return 1F;
    }

    @Override
    default @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return null;
    }

    @Override
    @Nullable
    default PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
        return null;
    }

    @Override
    default int getDensity() {
        return 0;
    }

    @Override
    default int getTemperature() {
        return 0;
    }

    @Override
    default int getViscosity() {
        return 0;
    }

    @Override
    default void setItemMovement(ItemEntity entity) {
        if (!entity.isNoGravity()) entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
    }
}
