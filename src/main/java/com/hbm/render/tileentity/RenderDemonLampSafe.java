package com.hbm.render.tileentity;

import com.hbm.hfr.render.loader.HFRWavefrontObject;
import com.hbm.lib.RefStrings;
import com.hbm.render.amlfrom1710.IModelCustom;
import com.hbm.tileentity.machine.TileEntityDemonLampSafe;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderDemonLampSafe extends TileEntitySpecialRenderer<TileEntityDemonLampSafe> {

	public static final IModelCustom demon_lamp = new HFRWavefrontObject(new ResourceLocation(RefStrings.MODID, "models/blocks/demon_lamp_safe.obj"));
	public static final ResourceLocation tex = new ResourceLocation(RefStrings.MODID, "textures/models/machines/demon_lamp.png");
	
	@Override
	public boolean isGlobalRenderer(TileEntityDemonLampSafe te){
		return true;
	}
	
	@Override
	public void render(TileEntityDemonLampSafe te, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.enableCull();

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		bindTexture(tex);
		demon_lamp.renderAll();

		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.depthMask(true);

		GL11.glPopMatrix();
	}
}
