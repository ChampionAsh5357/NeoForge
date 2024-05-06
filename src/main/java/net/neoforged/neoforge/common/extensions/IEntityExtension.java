/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.payload.AdvancedAddEntityPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public interface IEntityExtension extends INBTSerializable<CompoundTag> {
    private Entity self() {
        return (Entity) this;
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        self().load(nbt);
    }

    @Override
    default CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag ret = new CompoundTag();
        String id = self().getEncodeId();
        if (id != null) {
            ret.putString("id", self().getEncodeId());
        }
        return self().saveWithoutId(ret);
    }

    @Nullable
    Collection<ItemEntity> captureDrops();

    Collection<ItemEntity> captureDrops(@Nullable Collection<ItemEntity> captureDrops);

    /**
     * Returns a NBTTagCompound that can be used to store custom data for this entity.
     * It will be written, and read from disc, so it persists over world saves.
     * 
     * @return A NBTTagCompound
     */
    CompoundTag getPersistentData();

    /**
     * Used in model rendering to determine if the entity riding this entity should be in the 'sitting' position.
     * 
     * @return false to prevent an entity that is mounted to this entity from displaying the 'sitting' animation.
     */
    default boolean shouldRiderSit() {
        return true;
    }

    /**
     * Called when a user uses the creative pick block button on this entity.
     *
     * @param target The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, null ItemStack if nothing should be added.
     */
    @Nullable
    default ItemStack getPickedResult(HitResult target) {
        return self().getPickResult();
    }

    /**
     * If a rider of this entity can interact with this entity. Should return true on the
     * ridden entity if so.
     *
     * @return if the entity can be interacted with from a rider
     */
    default boolean canRiderInteract() {
        return false;
    }

    /**
     * Returns whether the entity can ride in this vehicle under the fluid.
     *
     * @param fluid  the fluid
     * @param rider the entity riding the vehicle
     * @return {@code true} if the vehicle can be ridden in under this fluid,
     *         {@code false} otherwise
     */
    default boolean canBeRiddenUnderFluid(Fluid fluid, Entity rider) {
        return fluid.canRideVehicleUnder(self(), rider);
    }

    /**
     * Checks if this {@link Entity} can trample a {@link Block}.
     *
     * @param pos          The block pos
     * @param fallDistance The fall distance
     * @return {@code true} if this entity can trample, {@code false} otherwise
     */
    boolean canTrample(BlockState state, BlockPos pos, float fallDistance);

    /**
     * Returns The classification of this entity
     * 
     * @param forSpawnCount If this is being invoked to check spawn count caps.
     * @return If the creature is of the type provided
     */
    default MobCategory getClassification(boolean forSpawnCount) {
        return self().getType().getCategory();
    }

    /**
     * Gets whether this entity has been added to a world (for tracking). Specifically
     * between the times when an entity is added to a world and the entity being removed
     * from the world's tracked lists.
     *
     * @return True if this entity is being tracked by a world
     */
    // TODO: rename in 1.19 to isAddedToLevel
    boolean isAddedToWorld();

    /**
     * Called after the entity has been added to the world's
     * ticking list. Can be overriden, but needs to call super
     * to prevent MC-136995.
     */
    // TODO: rename in 1.19 to onAddedToLevel
    void onAddedToWorld();

    /**
     * Called after the entity has been removed to the world's
     * ticking list. Can be overriden, but needs to call super
     * to prevent MC-136995.
     */
    // TODO: rename in 1.19 to onRemovedFromLevel
    void onRemovedFromWorld();

    /**
     * Revives an entity that has been removed from a world.
     * Used as replacement for entity.removed = true. Having it as a function allows
     * the entity to react to being revived.
     */
    void revive();

    /**
     * This is used to specify that your entity has multiple individual parts, such as the Vanilla Ender Dragon.
     *
     * See {@link EnderDragon} for an example implementation.
     * 
     * @return true if this is a multipart entity.
     */
    default boolean isMultipartEntity() {
        return false;
    }

    /**
     * Gets the individual sub parts that make up this entity.
     *
     * The entities returned by this method are NOT saved to the world in nay way, they exist as an extension
     * of their host entity. The child entity does not track its server-side(or client-side) counterpart, and
     * the host entity is responsible for moving and managing these children.
     *
     * Only used if {@link #isMultipartEntity()} returns true.
     *
     * See {@link EnderDragon} for an example implementation.
     * 
     * @return The child parts of this entity. The value to be returned here should be cached.
     */
    @Nullable
    default PartEntity<?>[] getParts() {
        return null;
    }

    /**
     * Returns the height of the fluid in relation to the bounding box of
     * the entity. If the entity is not in the fluid, then {@code 0}
     * is returned.
     *
     * @param fluid the fluid
     * @return the height of the fluid compared to the entity
     */
    double getFluidHeight(Fluid fluid);

    /**
     * Returns the fluid which is the highest on the bounding box of
     * the entity.
     *
     * @return the fluid which is the highest on the bounding box of
     *         the entity
     */
    Fluid getMaxHeightFluid();

    /**
     * Returns whether the entity is within the fluid of the state.
     *
     * @param state the state of the fluid
     * @return {@code true} if the entity is within the fluid of the
     *         state, {@code false} otherwise
     */
    default boolean isInFluid(FluidState state) {
        return this.isInFluid(state.getType());
    }

    /**
     * Returns whether the entity is within the fluid.
     *
     * @param fluid the fluid
     * @return {@code true} if the entity is within the fluid,
     *         {@code false} otherwise
     */
    default boolean isInFluid(Fluid fluid) {
        return this.getFluidHeight(fluid) > 0.0D;
    }

    /**
     * Returns whether any fluid the entity is currently in matches
     * the specified condition.
     *
     * @param predicate a test taking in the fluid and its height
     * @return {@code true} if a fluid meets the condition, {@code false}
     *         otherwise
     */
    default boolean isInFluid(BiPredicate<Fluid, Double> predicate) {
        return isInFluid(predicate, false);
    }

    /**
     * Returns whether the fluid the entity is currently in matches
     * the specified condition.
     *
     * @param predicate   a test taking in the fluid and its height
     * @param forAllTypes {@code true} if all fluid should match the
     *                    condition instead of at least one
     * @return {@code true} if a fluid meets the condition, {@code false}
     *         otherwise
     */
    boolean isInFluid(BiPredicate<Fluid, Double> predicate, boolean forAllTypes);

    /**
     * Returns whether the entity is in a fluid.
     *
     * @return {@code true} if the entity is in a fluid, {@code false} otherwise
     */
    boolean isInFluid();

    /**
     * Returns the fluid that is on the entity's eyes.
     *
     * @return the fluid that is on the entity's eyes
     */
    Fluid getEyeInFluid();

    /**
     * Returns whether the fluid is on the entity's eyes.
     *
     * @return {@code true} if the fluid is on the entity's eyes, {@code false} otherwise
     */
    default boolean isEyeInFluid(Fluid fluid) {
        return fluid == this.getEyeInFluid();
    }

    /**
     * Returns whether the entity can start swimming in the fluid.
     *
     * @return {@code true} if the entity can start swimming, {@code false} otherwise
     */
    default boolean canStartSwimming() {
        return !this.getEyeInFluid().isAir() && this.canSwimInFluid(this.getEyeInFluid()) && this.canSwimInFluid(this.self().level().getFluidState(this.self().blockPosition()).getType());
    }

    /**
     * Returns how much the velocity of the fluid should be scaled by
     * when applied to an entity.
     *
     * @param fluid the fluid
     * @return a scalar to multiply to the fluid velocity
     */
    default double getFluidMotionScale(Fluid fluid) {
        return fluid.motionScale(self());
    }

    /**
     * Returns whether the fluid can push an entity.
     *
     * @param fluid the fluid
     * @return {@code true} if the entity can be pushed by the fluid, {@code false} otherwise
     */
    default boolean isPushedByFluid(Fluid fluid) {
        return self().isPushedByFluid() && fluid.canPushEntity(self());
    }

    /**
     * Returns whether the entity can swim in the fluid.
     *
     * @param fluid the fluid
     * @return {@code true} if the entity can swim in the fluid, {@code false} otherwise
     */
    default boolean canSwimInFluid(Fluid fluid) {
        return fluid.canSwim(self());
    }

    /**
     * Returns whether the entity can be extinguished by this fluid.
     *
     * @param fluid the fluid
     * @return {@code true} if the entity can be extinguished, {@code false} otherwise
     */
    default boolean canFluidExtinguish(Fluid fluid) {
        return fluid.canExtinguish(self());
    }

    /**
     * Returns how much the fluid should scale the damage done to a falling
     * entity when hitting the ground per tick.
     *
     * <p>Implementation: If the entity is in many fluids, the smallest modifier
     * is applied.
     *
     * @param fluid the fluid
     * @return a scalar to multiply to the fall damage
     */
    default float getFluidFallDistanceModifier(Fluid fluid) {
        return fluid.getFallDistanceModifier(self());
    }

    /**
     * Returns whether the entity can be hydrated by this fluid.
     *
     * <p>Hydration is an arbitrary word which depends on the entity.
     *
     * @param fluid the fluid
     * @return {@code true} if the entity can be hydrated, {@code false}
     *         otherwise
     */
    default boolean canHydrateInFluid(Fluid fluid) {
        return fluid.canHydrate(self());
    }

    /**
     * Returns a sound to play when a certain action is performed by the
     * entity in the fluid. If no sound is present, then the sound will be
     * {@code null}.
     *
     * @param fluid   the fluid
     * @param action the action being performed
     * @return the sound to play when performing the action
     */
    @Nullable
    default SoundEvent getSoundFromFluid(Fluid fluid, SoundAction action) {
        return fluid.getSound(self(), action);
    }

    /**
     * Returns whether this {@link Entity} has custom outline rendering behavior which does
     * not use the existing automatic outline rendering based on {@link Entity#isCurrentlyGlowing()}
     * and the entity's team color.
     *
     * @param player the local player currently viewing this {@code Entity}
     * @return {@code true} to enable outline processing
     */
    default boolean hasCustomOutlineRendering(Player player) {
        return false;
    }

    /**
     * Sends the pairing data to the client.
     *
     * @param serverPlayer  The player to send the data to.
     * @param bundleBuilder Callback to add a custom payload to the packet.
     */
    default void sendPairingData(ServerPlayer serverPlayer, Consumer<CustomPacketPayload> bundleBuilder) {
        if (this instanceof IEntityWithComplexSpawn) {
            bundleBuilder.accept(new AdvancedAddEntityPayload(self()));
        }
    }

    /**
     * Copies the serialized attachments from another entity to this entity.
     *
     * @param other   the entity that attachments should be copied from
     * @param isDeath if {@code true}, only attachments with {@link AttachmentType.Builder#copyOnDeath()} set are copied;
     *                if {@code false}, all serializable attachments are copied.
     */
    default void copyAttachmentsFrom(Entity other, boolean isDeath) {
        AttachmentInternals.copyEntityAttachments(other, self(), isDeath);
    }
}
