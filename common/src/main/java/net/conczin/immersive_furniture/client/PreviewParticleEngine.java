package net.conczin.immersive_furniture.client;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.conczin.immersive_furniture.client.gui.FakeCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;
import org.joml.Matrix4fStack;

import java.util.*;

public class PreviewParticleEngine {
    private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(
            ParticleRenderType.TERRAIN_SHEET,
            ParticleRenderType.PARTICLE_SHEET_OPAQUE,
            ParticleRenderType.PARTICLE_SHEET_LIT,
            ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
            ParticleRenderType.CUSTOM
    );

    private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newIdentityHashMap();

    public PreviewParticleEngine() {
    }

    public void add(Particle particle) {
        this.particles.computeIfAbsent(particle.getRenderType(), particleRenderType -> EvictingQueue.create(16384)).add(particle);
    }

    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) {
            this.add(particle);
        }
    }

    public void tick() {
        this.particles.forEach((particleRenderType, queue) -> this.tickParticleList(queue));
    }

    private void tickParticleList(Collection<Particle> particles) {
        if (!particles.isEmpty()) {
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                particle.tick();
                if (particle.isAlive()) continue;
                iterator.remove();
            }
        }
    }

    public void render(PoseStack poseStack, LightTexture lightTexture, Camera camera, float partialTicks) {
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();

        Matrix4fStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushMatrix();
        viewStack.mul(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();

        for (ParticleRenderType particlerendertype : RENDER_ORDER) {
            Queue<Particle> queue = this.particles.get(particlerendertype);
            if (queue != null && !queue.isEmpty()) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = particlerendertype.begin(tesselator, textureManager);
                if (bufferbuilder != null) {
                    for (Particle particle : queue) {
                        particle.render(bufferbuilder, camera, partialTicks);
                    }
                    MeshData meshdata = bufferbuilder.build();
                    if (meshdata != null) {
                        BufferUploader.drawWithShader(meshdata);
                    }
                }
            }
        }

        viewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }

    public void renderParticles(GuiGraphics graphics, float yaw, float pitch, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        FakeCamera camera = new FakeCamera();
        camera.setup(minecraft.level, minecraft.player, false, false, partialTicks, yaw, pitch);

        render(graphics.pose(), minecraft.gameRenderer.lightTexture(), camera, partialTicks);
    }
}
