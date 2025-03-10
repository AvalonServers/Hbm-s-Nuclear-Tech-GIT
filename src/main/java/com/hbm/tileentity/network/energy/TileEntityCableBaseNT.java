package com.hbm.tileentity.network.energy;

import api.hbm.energy.IEnergyConductor;
import api.hbm.energy.Nodespace;
import com.hbm.lib.ForgeDirection;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCableBaseNT extends TileEntity implements IEnergyConductor {

	protected Nodespace.PowerNode node;

	public void update() {
		if (world.isRemote) return;

		if(this.node == null || this.node.expired) {
			if(this.shouldCreateNode()) {
				this.node = Nodespace.getNode(world, pos);

				if(this.node == null || this.node.expired) {
					this.node = this.createNode();
					Nodespace.createNode(world, this.node);
				}
			}
		}
	}

	public boolean canUpdate() {
		return (this.node == null || !this.node.hasValidNet()) && !this.isInvalid();
	}

	public boolean shouldCreateNode() {
		return true;
	}

	public Nodespace.PowerNode getNode() {
		return node;
	}

	public void onNodeDestroyedCallback() {
		this.node = null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		update();
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(!world.isRemote) {
			if(this.node != null) {
				Nodespace.destroyNode(world, pos);
			}
		}
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN;
	}
}
