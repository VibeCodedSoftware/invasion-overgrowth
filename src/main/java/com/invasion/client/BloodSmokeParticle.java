package com.invasion.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Tall, slow, long-lived red smoke column (uses the game's own big smoke sprites, tinted blood red). */
public class BloodSmokeParticle extends TextureSheetParticle {
    protected BloodSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z);
        scale(3.0F);
        setSize(0.25F, 0.25F);
        lifetime = 200 + random.nextInt(80);
        gravity = 3.0E-6F;
        xd = dx;
        yd = dy + random.nextDouble() / 500.0;
        zd = dz;
        float shade = 0.75F + random.nextFloat() * 0.25F;
        setColor(0.55F * shade, 0.04F * shade, 0.04F * shade);
        alpha = 0.9F;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime || alpha <= 0.0F) {
            remove();
            return;
        }
        xd += random.nextFloat() / 5000.0F * (random.nextBoolean() ? 1 : -1);
        zd += random.nextFloat() / 5000.0F * (random.nextBoolean() ? 1 : -1);
        yd -= gravity;
        move(xd, yd, zd);
        if (age >= lifetime - 60 && alpha > 0.01F) alpha -= 0.015F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            BloodSmokeParticle p = new BloodSmokeParticle(level, x, y, z, dx, dy, dz);
            p.pickSprite(sprites);
            return p;
        }
    }
}
