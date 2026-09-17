package com.moneybags.tempfly.aesthetic.particle;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParticlesTest {

    @Test
    public void testParseValidParticle() {
        Particle p = Particles.parseParticle("FLAME");
        assertEquals(Particle.FLAME, p);

        Particle lower = Particles.parseParticle("flame");
        assertEquals(Particle.FLAME, lower);
    }

    @Test
    public void testParseInvalidParticle() {
        Particle invalid = Particles.parseParticle("THIS_PARTICLE_DOES_NOT_EXIST_XYZ");
        assertNull(invalid);
    }

    @Test
    public void testParseNullOrEmpty() {
        assertNull(Particles.parseParticle(null));
        assertNull(Particles.parseParticle(""));
    }

    @Test
    public void testParticleCaching() {
        Particle p1 = Particles.parseParticle("HEART");
        Particle p2 = Particles.parseParticle("heart");

        assertNotNull(p1);
        assertSame(p1, p2);
    }
}
