/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.fluids.capability.wrappers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidBlockWrapper implements IFluidHandler {
    protected final IFluidBlock fluidBlock;
    protected final Level world;
    protected final BlockPos blockPos;

    public FluidBlockWrapper(IFluidBlock fluidBlock, Level world, BlockPos blockPos) {
        this.fluidBlock = fluidBlock;
        this.world = world;
        this.blockPos = blockPos;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? fluidBlock.drain(world, blockPos, FluidAction.SIMULATE) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        FluidStack stored = getFluidInTank(tank);
        if (!stored.isEmpty()) {
            float filledPercentage = fluidBlock.getFilledPercentage(world, blockPos);
            if (filledPercentage > 0) {
                return (int) (stored.getAmount() / filledPercentage);
            }
        }
        return IFluidExtension.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(fluidBlock.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fluidBlock.place(world, blockPos, resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!resource.isEmpty() && fluidBlock.canDrain(world, blockPos) && resource.is(fluidBlock.getFluid())) {
            FluidStack simulatedDrained = fluidBlock.drain(world, blockPos, FluidAction.SIMULATE);
            if (simulatedDrained.getAmount() <= resource.getAmount() && FluidStack.isSameFluidSameComponents(resource, simulatedDrained)) {
                if (action.execute()) {
                    return fluidBlock.drain(world, blockPos, FluidAction.EXECUTE).copy();
                }
                return simulatedDrained.copy();
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain > 0 && fluidBlock.canDrain(world, blockPos)) {
            FluidStack simulatedDrained = fluidBlock.drain(world, blockPos, FluidAction.SIMULATE);
            if (simulatedDrained.getAmount() <= maxDrain) {
                if (action.execute()) {
                    return fluidBlock.drain(world, blockPos, FluidAction.EXECUTE).copy();
                }
                return simulatedDrained.copy();
            }
        }
        return FluidStack.EMPTY;
    }
}
