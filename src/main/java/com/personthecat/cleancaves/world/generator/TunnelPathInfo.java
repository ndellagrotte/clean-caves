package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.data.RavineSettings;
import com.personthecat.cleancaves.data.TunnelSettings;
import com.personthecat.cleancaves.model.PrimerData;
import com.personthecat.cleancaves.model.ScalableFloat;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Holds all of the information related to the tunnel generator's current position along
 * the path of a tunnel. Used for generating a series of center coordinates around which
 * to generate spheres. Based on Mojang's original algorithm.
 */
public class TunnelPathInfo {

    private static final float PI_TIMES_2 = (float) (Math.PI * 2);

    /** Instructions for mutating values over time. */
    private final ScalableFloat sfYaw;
    private final ScalableFloat sfPitch;
    private final ScalableFloat sfdYaw;
    private final ScalableFloat sfdPitch;
    private final ScalableFloat sfScale;
    private final ScalableFloat sfStretch;

    /** The angles in radians for this tunnel. */
    private float yaw;
    private float pitch;

    /** The amount to alter angle(XZ/Y) per-segment. */
    private float deltaYaw;
    private float deltaPitch;
    private float scale;
    private float stretch;

    /** Coordinates of this tunnel's current destination. */
    private float x;
    private float y;
    private float z;

    /** Neatly constructs a new object based on values from tunnel settings. */
    public TunnelPathInfo(TunnelSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.yaw(), cfg.pitch(), cfg.deltaYaw(), cfg.deltaPitch(), cfg.scale(), cfg.stretch(), rand, destChunkX,
                destChunkZ, cfg.originHeight().min(), cfg.originHeight().max());
    }

    /** Neatly constructs a new object based on values from ravine settings. */
    public TunnelPathInfo(RavineSettings cfg, Random rand, int destChunkX, int destChunkZ) {
        this(cfg.yaw(), cfg.pitch(), cfg.deltaYaw(), cfg.deltaPitch(), cfg.scale(), cfg.stretch(), rand, destChunkX,
                destChunkZ, cfg.originHeight().min(), cfg.originHeight().max());
    }

    /** Used for handling initial encapsulation of inner values. */
    private TunnelPathInfo(ScalableFloat yaw, ScalableFloat pitch, ScalableFloat deltaYaw, ScalableFloat deltaPitch,
            ScalableFloat scale, ScalableFloat stretch, Random rand, int destChunkX, int destChunkZ, int minHeight,
            int maxHeight) {
        this.yaw = yaw.startVal() + yaw.startValRandFactor() * (rand.nextFloat() * PI_TIMES_2);
        this.pitch = pitch.startVal() + pitch.startValRandFactor() * (rand.nextFloat() - 0.50F);
        this.scale = scale.startVal() + scale.startValRandFactor() * (rand.nextFloat() * 2.00F + rand.nextFloat());
        this.deltaYaw = deltaYaw.startVal();
        this.deltaPitch = deltaPitch.startVal();
        this.stretch = stretch.startVal();
        this.sfYaw = yaw;
        this.sfPitch = pitch;
        this.sfdYaw = deltaYaw;
        this.sfdPitch = deltaPitch;
        this.sfScale = scale;
        this.sfStretch = stretch;
        // Random coordinates in the destination chunk.
        final int heightDiff = maxHeight - minHeight;
        this.x = (destChunkX * 16) + rand.nextInt(16);
        this.y = rand.nextInt(rand.nextInt(heightDiff) + 2) + minHeight;
        this.z = (destChunkZ * 16) + rand.nextInt(16);
    }

    /** Used for constructing updated or cloned instances. */
    private TunnelPathInfo(TunnelPathInfo from, float yaw, float pitch, float deltaYaw, float deltaPitch, float scale,
            float stretch, float x, float y, float z) {
        this.sfYaw = from.sfYaw;
        this.sfPitch = from.sfPitch;
        this.sfdYaw = from.sfdYaw;
        this.sfdPitch = from.sfdPitch;
        this.sfScale = from.sfScale;
        this.sfStretch = from.sfStretch;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.stretch = stretch;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Returns a new instance, resetting all primary fields to the input values. */
    public TunnelPathInfo reset(float angleXZ, float angleY, float scale, float scaleY) {
        return new TunnelPathInfo(this, angleXZ, angleY, sfdYaw.startVal(), sfdPitch.startVal(), scale, scaleY, x, y,
                z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getScale() {
        return scale;
    }

    public void multiplyScale(float by) {
        scale *= by;
    }

    public float getStretch() {
        return stretch;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void update(Random rand, boolean noiseYReduction, float angleYFactor, float twistPotential) {
        // Find the next position on a curvilinear path.
        nextPos();
        // Vertical noise control.
        if (noiseYReduction) {
            pitch *= angleYFactor;
        }
        // Recalculate the size / angle data for the next segment.
        updateVals(rand, twistPotential);
    }

    public boolean travelledTooFar(PrimerData data, int currentPos, int distance) {
        final double fromCenterX = x - data.centerX();
        final double fromCenterZ = z - data.centerZ();
        // Name? Is this related to Y?
        final double distanceRemaining = distance - currentPos;
        final double adjustedScale = scale + 18.00;

        final double fromCenterX2 = fromCenterX * fromCenterX;
        final double fromCenterZ2 = fromCenterZ * fromCenterZ;
        final double distanceRemaining2 = distanceRemaining * distanceRemaining;
        final double adjustedScale2 = adjustedScale * adjustedScale;

        return (fromCenterX2 + fromCenterZ2 - distanceRemaining2) > adjustedScale2;
    }

    public boolean touchesChunk(PrimerData data, double diameterXZ) {
        return x >= data.centerX() - 16.0 - diameterXZ
                && z >= data.centerZ() - 16.0 - diameterXZ
                && x <= data.centerX() + 16.0 + diameterXZ
                && z <= data.centerZ() + 16.0 + diameterXZ;
    }

    void nextPos() {
        final float cosPitch = MathHelper.cos(pitch);
        final float sinPitch = MathHelper.sin(pitch);
        x += MathHelper.cos(yaw) * cosPitch;
        y += sinPitch;
        z += MathHelper.sin(yaw) * cosPitch;
    }

    void updateVals(Random rand, float maxRotation) {
        // Adjust the angle based on current twist(XZ/Y). twist
        // will have been recalculated on subsequent iterations.
        // The potency of twist is reduced immediately.
        yaw += deltaYaw * maxRotation;
        pitch += deltaPitch * maxRotation;
        // Rotates the beginning of the chain around the end.
        deltaPitch = rotate(deltaPitch, rand, sfdPitch);
        // Positive is counterclockwise, negative is clockwise.
        deltaYaw = rotate(deltaYaw, rand, sfdYaw);
        scale = reScale(scale, rand, sfScale);
        stretch = reScale(stretch, rand, sfStretch);
    }

    /** Updates the value of `original` based on the input settings. */
    private float rotate(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent());
        original *= f.factor();
        original += f.randFactor() * (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat();
        return original;
    }

    /** Updates the value of `original` based on the input settings. */
    private float reScale(float original, Random rand, ScalableFloat f) {
        original = (float) Math.pow(original, f.exponent());
        original *= f.factor();
        //original += f.randFactor * (rand.nextFloat() - 0.5F);
        if (original < 0) {
            original = 0;
        }
        return original;
    }

}
