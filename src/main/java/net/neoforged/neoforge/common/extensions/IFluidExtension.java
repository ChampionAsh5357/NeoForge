/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidExtensions;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IFluidExtension {

    /**
     * The number of fluid units that a bucket represents.
     */
    int BUCKET_VOLUME = 1000;

    private Fluid self() {
        return (Fluid) this;
    }

    /* Default Accessors */

    /**
     * Returns the component representing the name of the fluid.
     *
     * @return the component representing the name of the fluid
     */
    default Component getDescription() {
        return Component.translatable(this.getDescriptionId());
    }

    /**
     * Returns the identifier representing the name of the fluid.
     * If no identifier was specified, then the identifier will be defaulted
     * to {@code fluid.<modid>.<registry_name>}.
     *
     * @return the identifier representing the name of the fluid
     */
    default String getDescriptionId() {
        return Util.makeDescriptionId("fluid", BuiltInRegistries.FLUID.getKey(self()));
    }

    /**
     * Returns the light level emitted by the fluid.
     *
     * <p>Note: This should be a value between {@code [0,15]}. If not specified, the
     * light level is {@code 0} as most fluids do not emit light.
     *
     * <p>Implementation: This is used by the bucket model to determine whether the fluid
     * should render full-bright when {@code applyFluidLuminosity} is {@code true}.
     *
     * @return the light level emitted by the fluid
     */
    default int getLightLevel() {
        return 0;
    }

    /**
     * Returns the density of the fluid.
     *
     * <p>Note: This is an arbitrary number. Negative or zero values indicate
     * that the fluid is lighter than air. If not specified, the density is
     * approximately equivalent to the real-life density of water in {@code kg/m^3}.
     *
     * @return the density of the fluid
     */
    default int getDensity() {
        return 1000;
    }

    /**
     * Returns the temperature of the fluid.
     *
     * <p>Note: This is an arbitrary number. Higher temperature values indicate
     * that the fluid is hotter. If not specified, the temperature is approximately
     * equivalent to the real-life room temperature of water in {@code Kelvin}.
     *
     * @return the temperature of the fluid
     */
    default int getTemperature() {
        return 300;
    }

    /**
     * Returns the viscosity, or thickness, of the fluid.
     *
     * <p>Note: This is an arbitrary number. The value should never be negative.
     * Higher viscosity values indicate that the fluid flows more slowly. If not
     * specified, the viscosity is approximately equivalent to the real-life
     * viscosity of water in {@code m/s^2}.
     *
     * @return the viscosity of the fluid
     */
    default int getViscosity() {
        return 1000;
    }

    /**
     * Returns the rarity of the fluid.
     *
     * <p>Note: If not specified, the rarity of the fluid is {@link Rarity#COMMON}.
     *
     * @return the rarity of the fluid
     */
    default Rarity getRarity() {
        return Rarity.COMMON;
    }

    /**
     * {@return the pointed dripstone drip information of the fluid}
     */
    @Nullable
    default DripstoneDripInfo getDripInfo() {
        return null;
    }

    /**
     * Returns whether the fluid can create a source.
     *
     * @return {@code true} if the fluid can create a source, {@code false} otherwise
     */
    default boolean canConvertToSource() {
        return false;
    }

    /**
     * Returns whether the fluid can hydrate.
     *
     * <p>Hydration is an arbitrary word which depends on the implementation.
     *
     * @return {@code true} if the fluid can hydrate, {@code false} otherwise
     */
    default boolean canHydrate() {
        return false;
    }

    /**
     * Returns whether the entity can be extinguished by this fluid.
     *
     * @return {@code true} if the entity can be extinguished, {@code false} otherwise
     */
    default boolean canExtinguish() {
        return false;
    }

    /**
     * Returns a sound to play when a certain action is performed. If no
     * sound is present, then the sound will be {@code null}.
     *
     * @param action the action being performed
     * @return the sound to play when performing the action
     */
    @Nullable
    default SoundEvent getSound(SoundAction action) {
        return null;
    }

    /* Entity-Based Accessors */

    /**
     * Returns how much the velocity of the fluid should be scaled by
     * when applied to an entity.
     *
     * @param entity the entity in the fluid
     * @return a scalar to multiply to the fluid velocity
     */
    default double motionScale(Entity entity) {
        return 0.014D;
    }

    /**
     * Returns whether the fluid can push an entity.
     *
     * @param entity the entity in the fluid
     * @return {@code true} if the entity can be pushed by the fluid, {@code false} otherwise
     */
    default boolean canPushEntity(Entity entity) {
        return true;
    }

    /**
     * Returns whether the entity can swim in the fluid.
     *
     * @param entity the entity in the fluid
     * @return {@code true} if the entity can swim in the fluid, {@code false} otherwise
     */
    default boolean canSwim(Entity entity) {
        return true;
    }

    /**
     * Returns how much the fluid should scale the damage done to a falling
     * entity when hitting the ground per tick.
     *
     * <p>Implementation: If the entity is in many fluids, the smallest modifier
     * is applied.
     *
     * @param entity the entity in the fluid
     * @return a scalar to multiply to the fall damage
     */
    default float getFallDistanceModifier(Entity entity) {
        return 0.5F;
    }

    /**
     * Returns whether the entity can be extinguished by this fluid.
     *
     * @param entity the entity in the fluid
     * @return {@code true} if the entity can be extinguished, {@code false} otherwise
     */
    default boolean canExtinguish(Entity entity) {
        return this.canExtinguish();
    }

    /**
     * Performs how an entity moves when within the fluid. If using custom
     * movement logic, the method should return {@code true}. Otherwise, the
     * movement logic will default to water.
     *
     * @param state          the state of the fluid
     * @param entity         the entity moving within the fluid
     * @param movementVector the velocity of how the entity wants to move
     * @param gravity        the gravity to apply to the entity
     * @return {@code true} if custom movement logic is performed, {@code false} otherwise
     */
    default boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        return false;
    }

    /**
     * Returns whether the entity can drown in the fluid.
     *
     * @param entity the entity in the fluid
     * @return {@code true} if the entity can drown in the fluid, {@code false} otherwise
     */
    default boolean canDrownIn(LivingEntity entity) {
        return true;
    }

    /**
     * Performs what to do when an item is in a fluid.
     *
     * @param entity the item in the fluid
     */
    default void setItemMovement(ItemEntity entity) {
        Vec3 vec3 = entity.getDeltaMovement();
        entity.setDeltaMovement(vec3.x * (double) 0.99F, vec3.y + (double) (vec3.y < (double) 0.06F ? 5.0E-4F : 0.0F), vec3.z * (double) 0.99F);
    }

    /**
     * Returns whether the boat can be used on the fluid.
     *
     * @param boat the boat trying to be used on the fluid
     * @return {@code true} if the boat can be used, {@code false} otherwise
     */
    default boolean supportsBoating(Boat boat) {
        return false;
    }

    /**
     * Returns whether the boat can be used on the fluid.
     *
     * @param state the state of the fluid
     * @param boat  the boat trying to be used on the fluid
     * @return {@code true} if the boat can be used, {@code false} otherwise
     */
    default boolean supportsBoating(FluidState state, Boat boat) {
        return this.supportsBoating(boat);
    }

    /**
     * Returns whether the entity can ride in this vehicle under the fluid.
     *
     * @param vehicle the vehicle being ridden in
     * @param rider   the entity riding the vehicle
     * @return {@code true} if the vehicle can be ridden in under this fluid,
     *         {@code false} otherwise
     */
    default boolean canRideVehicleUnder(Entity vehicle, Entity rider) {
        if (self().is(FluidTags.WATER)) return !vehicle.dismountsUnderwater();
        return true;
    }

    /**
     * Returns whether the entity can be hydrated by this fluid.
     *
     * <p>Hydration is an arbitrary word which depends on the entity.
     *
     * @param entity the entity in the fluid
     * @return {@code true} if the entity can be hydrated, {@code false}
     *         otherwise
     */
    default boolean canHydrate(Entity entity) {
        return this.canHydrate();
    }

    /**
     * Returns a sound to play when a certain action is performed by the
     * entity in the fluid. If no sound is present, then the sound will be
     * {@code null}.
     *
     * @param entity the entity in the fluid
     * @param action the action being performed
     * @return the sound to play when performing the action
     */
    @Nullable
    default SoundEvent getSound(Entity entity, SoundAction action) {
        return this.getSound(action);
    }

    /* Level-Based Accessors */

    /**
     * Returns whether the block can be extinguished by this fluid.
     *
     * @param state  the state of the fluid
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @return {@code true} if the block can be extinguished, {@code false} otherwise
     */
    default boolean canExtinguish(FluidState state, BlockGetter getter, BlockPos pos) {
        return this.canExtinguish();
    }

    /**
     * Returns whether the fluid can create a source.
     *
     * @param state  the state of the fluid
     * @param level the level that can get the fluid
     * @param pos    the location of the fluid
     * @return {@code true} if the fluid can create a source, {@code false} otherwise
     */
    default boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return this.canConvertToSource();
    }

    /**
     * Gets the path type of this fluid when an entity is pathfinding. When
     * {@code null}, uses vanilla behavior.
     *
     * @param state       the state of the fluid
     * @param level       the level which contains this fluid
     * @param pos         the position of the fluid
     * @param mob         the mob currently pathfinding, may be {@code null}
     * @param canFluidLog {@code true} if the path is being applied for fluids that can log blocks,
     *                    should be checked against if the fluid can log a block
     * @return the path type of this fluid
     */
    // TODO championash5357: Rename to getPathType
    @Nullable
    default PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return PathType.WATER;
    }

    /**
     * Gets the path type of the adjacent fluid to a pathfinding entity.
     * Path types with a negative malus are not traversable for the entity.
     * Pathfinding entities will favor paths consisting of a lower malus.
     * When {@code null}, uses vanilla behavior.
     *
     * @param state        the state of the fluid
     * @param level        the level which contains this fluid
     * @param pos          the position of the fluid
     * @param mob          the mob currently pathfinding, may be {@code null}
     * @param originalType the path type of the source the entity is on
     * @return the path type of this fluid
     */
    // TODO championash5357: Rename to getAdjacentPathType
    @Nullable
    default PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
        return PathType.WATER_BORDER;
    }

    /**
     * Returns a sound to play when a certain action is performed at a
     * position. If no sound is present, then the sound will be {@code null}.
     *
     * @param player the player listening to the sound
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @param action the action being performed
     * @return the sound to play when performing the action
     */
    @Nullable
    default SoundEvent getSound(@Nullable Player player, BlockGetter getter, BlockPos pos, SoundAction action) {
        return this.getSound(action);
    }

    /**
     * Returns whether the block can be hydrated by a fluid.
     *
     * <p>Hydration is an arbitrary word which depends on the block.
     * <ul>
     * <li>A farmland has moisture</li>
     * <li>A sponge can soak up the liquid</li>
     * <li>A coral can live</li>
     * </ul>
     *
     * @param state     the state of the fluid
     * @param getter    the getter which can get the fluid
     * @param pos       the position of the fluid
     * @param source    the state of the block being hydrated
     * @param sourcePos the position of the block being hydrated
     * @return {@code true} if the block can be hydrated, {@code false} otherwise
     */
    default boolean canHydrate(FluidState state, BlockGetter getter, BlockPos pos, BlockState source, BlockPos sourcePos) {
        return this.canHydrate();
    }

    /**
     * Returns the light level emitted by the fluid.
     *
     * <p>Note: This should be a value between {@code [0,15]}. If not specified, the
     * light level is {@code 0} as most fluids do not emit light.
     *
     * @param state  the state of the fluid
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @return the light level emitted by the fluid
     */
    default int getLightLevel(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.getLightLevel();
    }

    /**
     * Returns the density of the fluid.
     *
     * <p>Note: This is an arbitrary number. Negative or zero values indicate
     * that the fluid is lighter than air. If not specified, the density is
     * approximately equivalent to the real-life density of water in {@code kg/m^3}.
     *
     * @param state  the state of the fluid
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @return the density of the fluid
     */
    default int getDensity(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.getDensity();
    }

    /**
     * Returns the temperature of the fluid.
     *
     * <p>Note: This is an arbitrary number. Higher temperature values indicate
     * that the fluid is hotter. If not specified, the temperature is approximately
     * equivalent to the real-life room temperature of water in {@code Kelvin}.
     *
     * @param state  the state of the fluid
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @return the temperature of the fluid
     */
    default int getTemperature(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.getTemperature();
    }

    /**
     * Returns the viscosity, or thickness, of the fluid.
     *
     * <p>Note: This is an arbitrary number. The value should never be negative.
     * Higher viscosity values indicate that the fluid flows more slowly. If not
     * specified, the viscosity is approximately equivalent to the real-life
     * viscosity of water in {@code m/s^2}.
     *
     * @param state  the state of the fluid
     * @param getter the getter which can get the fluid
     * @param pos    the position of the fluid
     * @return the viscosity of the fluid
     */
    default int getViscosity(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.getViscosity();
    }

    /**
     * Returns whether a fluid above a pointed dripstone block can successfully fill a cauldron below.
     *
     * <p>If this will return {@code true}, this method will also do 3 things:
     * <ul>
     * <li>Set the cauldron below to the proper filled state as defined by the FluidType's {@link DripstoneDripInfo}</li>
     * <li>Send the BLOCK_CHANGE {@link GameEvent}</li>
     * <li>Play a sound as defined by the FluidType's {@link DripstoneDripInfo}</li>
     * </ul>
     *
     * @param level       the level the fluid is being placed in
     * @param cauldronPos the position of the cauldron this fluid is dripping into
     * @return {@code true} if a cauldron is successfully filled, {@code false} otherwise
     */
    default boolean handleCauldronDrip(Level level, BlockPos cauldronPos) {
        if (self() instanceof FlowingFluid flowing && self().isSource(flowing.getSource(false)) && this.getDripInfo() != null) {
            BlockState cauldronBlock = this.getDripInfo().filledCauldron().defaultBlockState();
            level.setBlockAndUpdate(cauldronPos, cauldronBlock);
            level.gameEvent(GameEvent.BLOCK_CHANGE, cauldronPos, GameEvent.Context.of(cauldronBlock));
            SoundEvent dripSound = this.getSound(null, level, cauldronPos, SoundActions.CAULDRON_DRIP);
            if (dripSound != null) {
                level.playSound(null, cauldronPos, dripSound, SoundSource.BLOCKS, 2.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            }
            return true;
        }
        return false;
    }

    /* Stack-Based Accessors */

    /**
     * Returns whether the fluid can create a source.
     *
     * @param stack the stack holding the fluid
     * @return {@code true} if the fluid can create a source, {@code false} otherwise
     */
    default boolean canConvertToSource(FluidStack stack) {
        return this.canConvertToSource();
    }

    /**
     * Returns a sound to play when a certain action is performed. If no
     * sound is present, then the sound will be {@code null}.
     *
     * @param stack  the stack holding the fluid
     * @param action the action being performed
     * @return the sound to play when performing the action
     */
    @Nullable
    default SoundEvent getSound(FluidStack stack, SoundAction action) {
        return this.getSound(action);
    }

    /**
     * Returns the component representing the name of the fluid.
     *
     * @param stack the stack holding the fluid
     * @return the component representing the name of the fluid
     */
    default Component getDescription(FluidStack stack) {
        return Component.translatable(this.getDescriptionId(stack));
    }

    /**
     * Returns the identifier representing the name of the fluid.
     * If no identifier was specified, then the identifier will be defaulted
     * to {@code fluid_type.<modid>.<registry_name>}.
     *
     * @param stack the stack holding the fluid
     * @return the identifier representing the name of the fluid
     */
    default String getDescriptionId(FluidStack stack) {
        return this.getDescriptionId();
    }

    /**
     * Returns whether the fluid can hydrate.
     *
     * <p>Hydration is an arbitrary word which depends on the implementation.
     *
     * @param stack the stack holding the fluid
     * @return {@code true} if the fluid can hydrate, {@code false} otherwise
     */
    default boolean canHydrate(FluidStack stack) {
        return this.canHydrate();
    }

    /**
     * Returns the light level emitted by the fluid.
     *
     * <p>Note: This should be a value between {@code [0,15]}. If not specified, the
     * light level is {@code 0} as most fluids do not emit light.
     *
     * @param stack the stack holding the fluid
     * @return the light level emitted by the fluid
     */
    default int getLightLevel(FluidStack stack) {
        return this.getLightLevel();
    }

    /**
     * Returns the density of the fluid.
     *
     * <p>Note: This is an arbitrary number. Negative or zero values indicate
     * that the fluid is lighter than air. If not specified, the density is
     * approximately equivalent to the real-life density of water in {@code kg/m^3}.
     *
     * @param stack the stack holding the fluid
     * @return the density of the fluid
     */
    default int getDensity(FluidStack stack) {
        return this.getDensity();
    }

    /**
     * Returns the temperature of the fluid.
     *
     * <p>Note: This is an arbitrary number. Higher temperature values indicate
     * that the fluid is hotter. If not specified, the temperature is approximately
     * equivalent to the real-life room temperature of water in {@code Kelvin}.
     *
     * @param stack the stack holding the fluid
     * @return the temperature of the fluid
     */
    default int getTemperature(FluidStack stack) {
        return this.getTemperature();
    }

    /**
     * Returns the viscosity, or thickness, of the fluid.
     *
     * <p>Note: This is an arbitrary number. The value should never be negative.
     * Higher viscosity values indicate that the fluid flows more slowly. If not
     * specified, the viscosity is approximately equivalent to the real-life
     * viscosity of water in {@code m/s^2}.
     *
     * @param stack the stack holding the fluid
     * @return the viscosity of the fluid
     */
    default int getViscosity(FluidStack stack) {
        return this.getViscosity();
    }

    /**
     * Returns the rarity of the fluid.
     *
     * <p>Note: If not specified, the rarity of the fluid is {@link Rarity#COMMON}.
     *
     * @param stack the stack holding the fluid
     * @return the rarity of the fluid
     */
    default Rarity getRarity(FluidStack stack) {
        return this.getRarity();
    }

    /* Helper Methods */

    /**
     * Returns whether the fluid represents air.
     *
     * @return {@code true} if the type represents air, {@code false} otherwise
     */
    // TODO championash5357: Supposed to be final
    default boolean isAir() {
        return self().isSame(Fluids.EMPTY);
    }

    /**
     * Returns whether the fluid is from vanilla.
     *
     * @return {@code true} if the type is from vanilla, {@code false} otherwise
     */
    // TODO championash5357: Supposed to be final
    default boolean isVanilla() {
        return self().is(FluidTags.WATER) || self().is(FluidTags.LAVA);
    }

    /**
     * Returns the bucket containing the fluid.
     *
     * @param stack the stack holding the fluid
     * @return the bucket containing the fluid
     */
    default ItemStack getBucket(FluidStack stack) {
        return new ItemStack(stack.getFluid().getBucket());
    }

    /**
     * Returns the associated {@link BlockState} for a {@link FluidState}.
     *
     * @param getter the getter which can get the level data
     * @param pos    the position of where the fluid would be
     * @param state  the state of the fluid
     * @return the {@link BlockState} of a fluid
     */
    default BlockState getBlockForFluidState(BlockAndTintGetter getter, BlockPos pos, FluidState state) {
        return state.createLegacyBlock();
    }

    /**
     * Returns the {@link FluidState} when a {@link FluidStack} is trying to
     * place it.
     *
     * @param getter the getter which can get the level data
     * @param pos    the position of where the fluid is being placed
     * @param stack  the stack holding the fluid
     * @return the {@link FluidState} being placed
     */
    default FluidState getStateForPlacement(BlockAndTintGetter getter, BlockPos pos, FluidStack stack) {
        return stack.getFluid().defaultFluidState();
    }

    /**
     * Returns whether the fluid can be placed in the level.
     *
     * @param getter the getter which can get the level data
     * @param pos    the position of where the fluid is being placed
     * @param state  the state of the fluid being placed
     * @return {@code true} if the fluid can be placed, {@code false} otherwise
     */
    // TODO championash5357: Supposed to be final
    default boolean canBePlacedInLevel(BlockAndTintGetter getter, BlockPos pos, FluidState state) {
        return !this.getBlockForFluidState(getter, pos, state).isAir();
    }

    /**
     * Returns whether the fluid can be placed in the level.
     *
     * @param getter the getter which can get the level data
     * @param pos    the position of where the fluid is being placed
     * @param stack  the stack holding the fluid
     * @return {@code true} if the fluid can be placed, {@code false} otherwise
     */
    // TODO championash5357: Supposed to be final
    default boolean canBePlacedInLevel(BlockAndTintGetter getter, BlockPos pos, FluidStack stack) {
        return this.canBePlacedInLevel(getter, pos, this.getStateForPlacement(getter, pos, stack));
    }

    /**
     * Returns whether a fluid is lighter than air. If the fluid's density
     * is lower than or equal {@code 0}, the fluid is considered lighter than air.
     *
     * <p>Tip: {@code 0} is the "canonical" density of air within Forge.
     *
     * <p>Note: Fluids lighter than air will have their bucket model rotated
     * upside-down; fluid block models will have their vertices inverted.
     *
     * @return {@code true} if the fluid is lighter than air, {@code false} otherwise
     */
    // TODO championash5357: Supposed to be final
    default boolean isLighterThanAir() {
        return this.getDensity() <= 0;
    }

    /**
     * Determines if this fluid should be vaporized when placed into a level.
     *
     * <p>Note: Fluids that can turn lava into obsidian should vaporize within
     * the nether to preserve the intentions of vanilla.
     *
     * @param level the level the fluid is being placed in
     * @param pos   the position to place the fluid at
     * @param stack the stack holding the fluid being placed
     * @return {@code true} if this fluid should be vaporized on placement, {@code false} otherwise
     *
     * @see BucketItem#emptyContents(Player, Level, BlockPos, BlockHitResult)
     */
    default boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
        if (level.dimensionType().ultraWarm()) {
            return this.getStateForPlacement(level, pos, stack).is(FluidTags.WATER);
        }
        return false;
    }

    /**
     * Performs an action when a fluid can be vaporized when placed into a level.
     *
     * <p>Note: The fluid will already have been drained from the stack.
     *
     * @param player the player placing the fluid, may be {@code null} for blocks like dispensers
     * @param level  the level the fluid is vaporized in
     * @param pos    the position the fluid is vaporized at
     * @param stack  the stack holding the fluid being vaporized
     *
     * @see BucketItem#emptyContents(Player, Level, BlockPos, BlockHitResult)
     */
    default void onVaporize(@Nullable Player player, Level level, BlockPos pos, FluidStack stack) {
        SoundEvent sound = this.getSound(player, level, pos, SoundActions.FLUID_VAPORIZE);
        level.playSound(player, pos, sound != null ? sound : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

        for (int l = 0; l < 8; ++l)
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, (double) pos.getX() + Math.random(), (double) pos.getY() + Math.random(), (double) pos.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
    }

    /**
     * Returns the explosion resistance of the fluid.
     *
     * @param state     the state of the fluid
     * @param level     the level the fluid is in
     * @param pos       the position of the fluid
     * @param explosion the explosion the fluid is absorbing
     * @return the amount of the explosion the fluid can absorb
     */
    @SuppressWarnings("deprecation")
    default float getExplosionResistance(FluidState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getExplosionResistance();
    }

    default void initializeClient(Consumer<IClientFluidExtensions> consumer) {}

    /**
     * A record that holds some information to let a fluid drip from Pointed Dripstone stalactites and fill cauldrons below.
     *
     * @param chance         the chance that the cauldron below will be filled every time the Pointed Dripstone is randomly ticked. This number should be some value between 0.0 and 1.0
     * @param dripParticle   the particle that spawns randomly from the tip of the Pointed Dripstone when this fluid is above it
     * @param filledCauldron the block the Pointed Dripstone should replace an empty cauldron with when it successfully tries to fill the cauldron
     */
    record DripstoneDripInfo(float chance, @Nullable ParticleOptions dripParticle, Block filledCauldron) {}
}
