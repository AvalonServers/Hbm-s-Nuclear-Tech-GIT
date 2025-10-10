package com.hbm.sound;

import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class SoundLoopMachine extends PositionedSound implements ITickableSound {
	boolean donePlaying = false;
	TileEntity te;

    // TODO: this sound loop needs a hard cutoff because right now it always keeps playing in the background even if silent
	public SoundLoopMachine(SoundEvent path, TileEntity te) {
		super(path, SoundCategory.BLOCKS);
		this.repeat = true;
		this.volume = 1;
		this.pitch = 1;
		this.xPosF = te.getPos().getX();
		this.yPosF = te.getPos().getY();
		this.zPosF = te.getPos().getZ();
		this.repeatDelay = 0;
		this.te = te;

        update();
	}

	@Override
	public void update() {
        if (donePlaying) return;

		if(te == null || te.isInvalid()) {
            donePlaying = true;
            return;
        }

        if(te instanceof TileEntityLoadedBase && !((TileEntityLoadedBase) te).isLoaded()) {
            donePlaying = true;
            return;
        }

        float f;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if(player != null) {
            f = (float)Math.sqrt(Math.pow(xPosF - player.posX, 2) + Math.pow(yPosF - player.posY, 2) + Math.pow(zPosF - player.posZ, 2));
            volume = (f / 50) * -2 + 2;
        } else {
            volume = 1;
        }
	}

	@Override
	public boolean isDonePlaying() {
		return this.donePlaying;
	}
	
	public void setVolume(float f) {
		volume = f;
	}
	
	public void setPitch(float f) {
		pitch = f;
	}
	
	public void stop() {
		donePlaying = true;
	}
}