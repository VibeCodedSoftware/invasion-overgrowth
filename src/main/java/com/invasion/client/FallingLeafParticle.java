package com.invasion.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A single blood leaf drifting and swaying down from Blood Oak leaves. */
public class FallingLeafParticle extends TextureSheetParticle {
    private final float swayPhase, swaySpeed;

    protected FallingLeafParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        lifetime = 200;
        gravity = 0.0F;
        quadSize = 0.08F + random.nextFloat() * 0.04F;
        swayPhase = random.nextFloat() * Mth.TWO_PI;
        swaySpeed = 0.06F + random.nextFloat() * 0.05F;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        xd = zd = 0;
        yd = -0.04;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        oRoll = roll;
        if (age++ >= lifetime || onGround) { if (onGround && age < lifetime - 40) age = lifetime - 40; else if (age >= lifetime) { remove(); return; } }
        float s = Mth.sin(age * swaySpeed + swayPhase);
        xd = s * 0.03;
        zd = Mth.cos(age * swaySpeed * 0.7F + swayPhase) * 0.02;
        yd = Math.max(yd - 0.002, -0.06);
        roll += s * 0.05F;
        move(xd, yd, zd);
        if (age > lifetime - 40) alpha = (lifetime - age) / 40.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            FallingLeafParticle p = new FallingLeafParticle(level, x, y, z);
            p.pickSprite(sprites);
            return p;
        }
    }
}
