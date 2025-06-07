package immersive_furniture.client;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import immersive_furniture.client.gui.FakeCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;

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
        PoseStack viewStack = RenderSystem.getModelViewStack();
        viewStack.pushPose();
        viewStack.mulPoseMatrix(poseStack.last().pose());

        RenderSystem.applyModelViewMatrix();

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();

        for (ParticleRenderType particleRenderType : RENDER_ORDER) {
            Iterable<Particle> iterable = this.particles.get(particleRenderType);
            if (iterable == null) continue;
            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();
            particleRenderType.begin(bufferBuilder, textureManager);
            for (Particle particle : iterable) {
                particle.render(bufferBuilder, camera, partialTicks);
            }
            particleRenderType.end(tesselator);
        }
        viewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }

    public void renderParticles(GuiGraphics context, float yaw, float pitch, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        FakeCamera camera = new FakeCamera();
        camera.setup(minecraft.level, minecraft.player, false, false, partialTicks, yaw, pitch);

        render(context.pose(), minecraft.gameRenderer.lightTexture(), camera, partialTicks);

        context.flush();
    }
}
