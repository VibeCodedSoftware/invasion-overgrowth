package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.ModRegistry;
import com.invasion.ModFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.invasion.entity.EntEntity;
import com.invasion.entity.MeteorEntity;
import com.invasion.item.GloveCharge;
import com.invasion.item.ParticleGloveItem;
import com.invasion.network.CycleElementPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = InvasionMod.MODID, dist = Dist.CLIENT)
public class InvasionClient {
    static final KeyMapping CYCLE_ELEMENT = new KeyMapping("key.invasion.cycle_element", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.invasion");

    public InvasionClient(IEventBus modBus) {
        modBus.addListener(InvasionClient::registerRenderers);
        modBus.addListener(InvasionClient::registerLayers);
        modBus.addListener(InvasionClient::registerShaders);
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(CYCLE_ELEMENT));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) -> {
            e.registerSpriteSet(ModRegistry.BLOOD_SMOKE.get(), BloodSmokeParticle.Provider::new);
            e.registerSpriteSet(ModRegistry.FALLING_BLOOD_LEAF.get(), FallingLeafParticle.Provider::new);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) -> e.register((stack, tint) -> tint == 0 ? 0xFFB54F66 : -1, com.invasion.ModWood.BOAT.get(), com.invasion.ModWood.CHEST_BOAT.get()));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) -> {
            e.register((state, level, pos, tint) -> 0x8E0C0C, ModRegistry.BLOOD_GEYSER.get());
            e.register((state, level, pos, tint) -> net.minecraft.world.level.FoliageColor.getDefaultColor(), ModRegistry.GIANT_OAK_SAPLING.get());
            e.register((state, level, pos, tint) -> net.minecraft.world.level.FoliageColor.getBirchColor(), ModRegistry.GIANT_BIRCH_SAPLING.get());
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) -> {
            e.register((stack, tint) -> tint == 0 ? 0xFF000000 | net.minecraft.world.level.FoliageColor.getDefaultColor() : -1, ModRegistry.GIANT_OAK_SAPLING_ITEM.get());
            e.register((stack, tint) -> tint == 0 ? 0xFF000000 | net.minecraft.world.level.FoliageColor.getBirchColor() : -1, ModRegistry.GIANT_BIRCH_SAPLING_ITEM.get());
            e.register(new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors(), ModFluids.BLOOD_BUCKET.get());
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, InvasionMod.id("glove_tanks"), InvasionClient::renderGloveHud));
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new RuntimeSkins()));
        modBus.addListener((RegisterClientExtensionsEvent e) -> e.registerFluidType(new BloodFluidExtensions(), ModFluids.BLOOD_TYPE.get()));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.BLOOD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_BLOOD.get(), RenderType.translucent());
        }));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (CYCLE_ELEMENT.consumeClick()) {
                LocalPlayer p = Minecraft.getInstance().player;
                if (p != null && (p.getMainHandItem().getItem() instanceof ParticleGloveItem || p.getOffhandItem().getItem() instanceof ParticleGloveItem)) {
                    PacketDistributor.sendToServer(CycleElementPayload.INSTANCE);
                }
            }
        });
    }

    private static void registerShaders(RegisterShadersEvent e) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(), InvasionMod.id("rendertype_invasion_portal"),
                    DefaultVertexFormat.POSITION), shader -> InvasionRenderTypes.portalShader = shader);
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException("Failed to load Invasion portal shader", ex);
        }
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(EntModel.LAYER, EntModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModRegistry.PORTAL_CORE.get(), PortalRenderer::new);
        e.registerEntityRenderer(ModRegistry.INVADER.get(), ctx -> new TexturedZombieRenderer(ctx, InvasionMod.id("textures/entity/invader.png")));
        e.registerEntityRenderer(ModRegistry.INVADER_MINER.get(), ctx -> new TexturedZombieRenderer(ctx, InvasionMod.id("textures/entity/invader_miner.png")));
        e.registerEntityRenderer(ModRegistry.GUARDIAN.get(), GuardianRenderer::new);
        e.registerEntityRenderer(ModRegistry.METEOR.get(), MeteorRenderer::new);
        e.registerEntityRenderer(ModRegistry.ENT.get(), EntRenderer::new);
        e.registerEntityRenderer(com.invasion.ModFlesh.FLESHLING.get(), ctx -> new MeshEntityRenderer<>(ctx, "fleshling", 0.6F, 1.8F));
        e.registerEntityRenderer(com.invasion.ModFlesh.FLESH_THING.get(), ctx -> new MeshEntityRenderer<>(ctx, "flesh_thing", 1.6F));
        e.registerEntityRenderer(com.invasion.ModFlesh.BLOOD_GLOB.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    // ------------------------------------------------------------------ gauntlet HUD
    private static void renderGloveHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        ItemStack stack = p.getMainHandItem().getItem() instanceof ParticleGloveItem ? p.getMainHandItem()
                : p.getOffhandItem().getItem() instanceof ParticleGloveItem ? p.getOffhandItem() : ItemStack.EMPTY;
        if (stack.isEmpty()) return;
        GloveCharge c = ParticleGloveItem.charge(stack);
        int x = g.guiWidth() / 2 + 98, y = g.guiHeight() - 34;
        ItemStack[] icons = {new ItemStack(Items.TORCH), new ItemStack(Items.REDSTONE_TORCH), new ItemStack(Items.WATER_BUCKET)};
        GloveCharge.Element[] els = GloveCharge.Element.values();
        for (int i = 0; i < 3; i++) {
            int row = y + i * 11;
            boolean selected = c.element() == els[i];
            g.pose().pushPose();
            g.pose().translate(x, row - 1, 0);
            g.pose().scale(0.625F, 0.625F, 1);
            g.renderItem(icons[i], 0, 0);
            g.pose().popPose();
            int bx = x + 12, bw = 52;
            g.fill(bx - 1, row + 1, bx + bw + 1, row + 8, selected ? 0xFFFFFFFF : 0xFF2A2A2A);
            g.fill(bx, row + 2, bx + bw, row + 7, 0xFF101010);
            int fill = Math.round(bw * c.get(els[i]) / (float) GloveCharge.MAX);
            g.fill(bx, row + 2, bx + fill, row + 7, 0xFF000000 | els[i].color);
            g.fill(bx, row + 2, bx + fill, row + 3, 0x40FFFFFF);
        }
    }

    // ------------------------------------------------------------------ renderers
    static class TexturedZombieRenderer extends ZombieRenderer {
        private final ResourceLocation texture;

        TexturedZombieRenderer(EntityRendererProvider.Context ctx, ResourceLocation texture) {
            super(ctx);
            this.texture = texture;
        }

        @Override
        public ResourceLocation getTextureLocation(Zombie entity) {
            return texture;
        }
    }

    /** Mossy-cobblestone guardian: skin is built from the player's own mossy cobblestone texture. */
    static class GuardianRenderer extends ZombieRenderer {
        GuardianRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(Zombie entity) {
            return RuntimeSkins.get("temple_guardian", ResourceLocation.withDefaultNamespace("textures/block/mossy_cobblestone.png"),
                    InvasionMod.id("textures/entity/temple_guardian_overlay.png"));
        }
    }

    static class EntRenderer extends MobRenderer<EntEntity, EntModel> {
        EntRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new EntModel(ctx.bakeLayer(EntModel.LAYER)), 1.1F);
            addLayer(new CrownLayer(this, ctx.getBlockRenderDispatcher()));
        }

        @Override
        public ResourceLocation getTextureLocation(EntEntity entity) {
            String log = entity.kind() == EntEntity.Kind.BIRCH ? "birch_log" : "oak_log";
            return RuntimeSkins.get("ent_" + log, ResourceLocation.withDefaultNamespace("textures/block/" + log + ".png"),
                    InvasionMod.id("textures/entity/ent_overlay.png"));
        }

        @Override
        protected void scale(EntEntity entity, PoseStack pose, float partialTick) {
            pose.scale(0.75F, 0.75F, 0.75F);
        }
    }

    /** The Ent's crown is made of real leaf blocks, so it always matches the game's leaves. */
    static class CrownLayer extends RenderLayer<EntEntity, EntModel> {
        private final BlockRenderDispatcher blocks;

        CrownLayer(RenderLayerParent<EntEntity, EntModel> parent, BlockRenderDispatcher blocks) {
            super(parent);
            this.blocks = blocks;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, EntEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            EntModel model = getParentModel();
            BlockState leaves = switch (entity.kind()) {
                case OAK -> Blocks.OAK_LEAVES.defaultBlockState();
                case BIRCH -> Blocks.BIRCH_LEAVES.defaultBlockState();
                case AZALEA -> Blocks.AZALEA_LEAVES.defaultBlockState();
            };
            BlockState flowering = entity.kind() == EntEntity.Kind.AZALEA ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState() : leaves;
            pose.pushPose();
            model.root().translateAndRotate(pose);
            model.torso().translateAndRotate(pose);
            model.head().translateAndRotate(pose);
            model.crown().translateAndRotate(pose);
            float c = 12.0F / 16.0F;
            for (int i = -2; i <= 1; i++) {
                for (int j = -2; j <= 1; j++) {
                    boolean corner = (i == -2 || i == 1) && (j == -2 || j == 1);
                    if (!corner) cube(pose, buffers, light, (i + j) % 2 == 0 ? flowering : leaves, i * c, -c, j * c, c);
                }
            }
            for (int i = -1; i <= 0; i++) for (int j = -1; j <= 0; j++) cube(pose, buffers, light, (i == j) ? flowering : leaves, i * c, -2 * c, j * c, c);
            pose.popPose();
        }

        private void cube(PoseStack pose, MultiBufferSource buffers, int light, BlockState state, float x, float y, float z, float size) {
            pose.pushPose();
            pose.translate(x, y, z);
            pose.scale(size, size, size);
            blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** Renders the meteor as a giant, tumbling, full-bright bloody cobblestone block. */
    static class MeteorRenderer extends EntityRenderer<MeteorEntity> {
        private final BlockRenderDispatcher blocks;

        MeteorRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.blocks = ctx.getBlockRenderDispatcher();
            this.shadowRadius = 0;
        }

        @Override
        public void render(MeteorEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float t = entity.tickCount + partialTick;
            pose.pushPose();
            pose.translate(0, 1.5, 0);
            pose.mulPose(Axis.YP.rotationDegrees(t * 8));
            pose.mulPose(Axis.XP.rotationDegrees(t * 11));
            float s = 4.0F;
            pose.scale(s, s, s);
            pose.translate(-0.5, -0.5, -0.5);
            blocks.renderSingleBlock(ModRegistry.BLOODY_COBBLESTONE.get().defaultBlockState(), pose, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            super.render(entity, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public boolean shouldRender(MeteorEntity entity, Frustum frustum, double x, double y, double z) {
            return true;
        }

        @Override
        public ResourceLocation getTextureLocation(MeteorEntity entity) {
            return InventoryMenu.BLOCK_ATLAS;
        }
    }
}
