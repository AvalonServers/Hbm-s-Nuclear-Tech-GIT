package com.hbm.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.Spaghetti;
import com.hbm.lib.ItemStackHandlerWrapper;
import com.hbm.lib.Library;
import com.hbm.packet.NBTPacket;
import com.hbm.packet.PacketDispatcher;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Random;

@Spaghetti("Not spaghetti in itself, but for the love of god please use this base class for all machines")
public abstract class TileEntityMachineBase extends TileEntityLoadedBase implements INBTPacketReceiver {
	public static final Random rand = new Random();
	public final int updateOffset = rand.nextInt(20);
	private boolean networkDirty = false;
	public int networkUpdateFrequency = 20;

	public ItemStackHandler inventory;

	private String customName;

	public TileEntityMachineBase(int scount) {
		this(scount, 64);
	}

	public TileEntityMachineBase(int scount, int slotlimit) {
		inventory = getNewInventory(scount, slotlimit);
	}

	public ItemStackHandler getNewInventory(int scount, int slotlimit){
		return new ItemStackHandler(scount){
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				markDirty();
			}
			
			@Override
			public int getSlotLimit(int slot) {
				return slotlimit;
			}
		};
	}
	
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : getName();
	}

	public abstract String getName();

	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}
	
	public void setCustomName(String name) {
		this.customName = name;
	}
	
	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this)
		{
			return false;
		}else{
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=128;
		}
	}
	
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {};
	}
	
	public int getGaugeScaled(int i, FluidTank tank) {
		return tank.getFluidAmount() * i / tank.getCapacity();
	}

	public void networkPack(NBTTagCompound nbt, int range) {
		if(!world.isRemote)
			PacketDispatcher.wrapper.sendToAllAround(new NBTPacket(nbt, pos), new TargetPoint(this.world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), range));
	}

	public void setNetworkDirty() {
		if (world.isRemote) return;
		networkDirty = true;
	}

	public boolean shouldSendNetworkUpdate() {
		if (world.isRemote) return false;

		if (networkDirty) {
			networkDirty = false;
			return true;
		}

		// VERTEX: Testing sending updates to the client only once per second for perf, TODO make this use wall clock time? doesn't really matter though since it's tq excluded
		return (world.getTotalWorldTime() + updateOffset) % 20 == 0;
	}
	
	public void networkUnpack(NBTTagCompound nbt) { }
	
	public void handleButtonPacket(int value, int meta) {
		setNetworkDirty();
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());
		return super.writeToNBT(compound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		super.readFromNBT(compound);
	}
	
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		return true;
	}
	
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		return this.isItemValidForSlot(slot, itemStack);
	}

	public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
		return true;
	}
	
	public int countMufflers() {

		int count = 0;

		for(EnumFacing dir : EnumFacing.VALUES)
			if(world.getBlockState(pos.offset(dir)).getBlock() == ModBlocks.muffler)
				count++;

		return count;
	}

	public float getVolume(int toSilence) {

		float volume = 1 - (countMufflers() / (float)toSilence);

		return Math.max(volume, 0);
	}

	//Unloads output into chests. Capability version.
	public boolean tryFillContainerCap(IItemHandler chest, int slot) {
		//Check if we have something to output
		if(inventory.getStackInSlot(slot).isEmpty())
			return false;

		for(int i = 0; i < chest.getSlots(); i++) {
			ItemStack outputStack = inventory.getStackInSlot(slot);
			if(outputStack.isEmpty())
				return false;

			ItemStack chestItem = chest.getStackInSlot(i);
			if(chestItem.isEmpty() || (Library.areItemStacksCompatible(outputStack, chestItem, false) && chestItem.getCount() < chestItem.getMaxStackSize())) {
				// VERTEX: what the fuck was the old version of this code? what the actual fuck?
				inventory.setStackInSlot(slot, chest.insertItem(i, outputStack, false));
				if (outputStack.isEmpty()) return true;
			}
		}

		return false;
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inventory != null){
			if(facing == null)
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new ItemStackHandlerWrapper(inventory, getAccessibleSlotsFromSide(facing)){
				@Override
				public ItemStack extractItem(int slot, int amount, boolean simulate) {
					if(canExtractItem(slot, inventory.getStackInSlot(slot), amount))
						return super.extractItem(slot, amount, simulate);
					return ItemStack.EMPTY;
				}
				
				@Override
				public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
					if(canInsertItem(slot, stack, stack.getCount()))
						return super.insertItem(slot, stack, simulate);
					return stack;
				}
			});
		}
		return super.getCapability(capability, facing);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inventory != null) || super.hasCapability(capability, facing);
	}
}
