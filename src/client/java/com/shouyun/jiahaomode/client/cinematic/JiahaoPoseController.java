// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import java.util.Map;
import java.util.WeakHashMap;

public final class JiahaoPoseController {
    private static final Map<PlayerEntityModel<?>, ModelTransform[]> SAVED = new WeakHashMap<>();
    // Head, torso, right arm, left arm, right leg, left leg; pitch/yaw/roll in degrees.
    private static final double[][][] POSES = {
        {{20,0,0},{5,0,0},{6,0,-5},{6,0,5},{0,0,0},{0,0,0}},
        {{5,-10,-4},{3,-5,-4},{-100,-20,30},{22,8,-12},{0,0,0},{0,0,0}},
        {{0,0,3},{0,20,5},{-65,-25,38},{16,12,-10},{0,0,0},{0,0,0}},
        {{-12,-5,-4},{-8,0,-3},{-112,-30,22},{8,0,-8},{0,0,0},{0,0,0}}
    };
    private JiahaoPoseController() { }
    private static ModelPart[] parts(PlayerEntityModel<?> model) {
        return new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.hat, model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants};
    }
    public static void restore(PlayerEntityModel<?> model) {
        ModelTransform[] state = SAVED.remove(model);
        if (state == null) return;
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length; i++) parts[i].setTransform(state[i]);
    }
    public static void apply(PlayerEntityModel<?> model, PlayerEntity player) {
        var dodge = com.shouyun.jiahaomode.client.JiahaoDodgeClientController.visual(player);
        boolean cinematic = JiahaoCinematicController.isPoseActive(player);
        if (!cinematic && dodge == null) return;
        ModelPart[] parts = parts(model);
        ModelTransform[] saved = new ModelTransform[parts.length];
        for (int i = 0; i < parts.length; i++) saved[i] = parts[i].getTransform();
        SAVED.put(model, saved);
        if (!cinematic) {
            double weight = dodge.weight();
            double tilt = Math.toRadians(dodge.perfect ? 25 : 17) * weight;
            model.body.roll += (float)(-dodge.sideways * tilt);
            model.body.pitch += (float)(dodge.forward * tilt * .6);
            model.rightArm.roll += (float)(.35 * weight); model.leftArm.roll -= (float)(.35 * weight);
            model.rightArm.pitch -= (float)(.15 * weight); model.leftArm.pitch -= (float)(.15 * weight);
            model.head.roll -= (float)(model.body.roll * .35);
            if (dodge.perfect && dodge.attackPosition != null) {
                var source = dodge.attackPosition.subtract(player.getPos());
                double sourceYaw = Math.toDegrees(Math.atan2(-source.x, source.z));
                float relative = MathHelper.clamp(MathHelper.wrapDegrees((float)sourceYaw - player.bodyYaw), -55, 55);
                model.head.yaw = (float)CinematicTimeline.lerp(model.head.yaw, Math.toRadians(relative), weight * .55);
            }
            model.hat.copyTransform(model.head); model.jacket.copyTransform(model.body);
            model.rightSleeve.copyTransform(model.rightArm); model.leftSleeve.copyTransform(model.leftArm);
            model.rightPants.copyTransform(model.rightLeg); model.leftPants.copyTransform(model.leftLeg);
            return;
        }
        double time = JiahaoCinematicController.elapsedTicks();
        int from = time < 24 ? 0 : time < 50 ? 0 : time < 72 ? 1 : 2;
        int to = time < 24 ? 0 : from + 1;
        double blend = time < 24 ? 0 : CinematicTimeline.smooth(time < 50 ? (time-24)/26 : time < 72 ? (time-50)/22 : (time-72)/16);
        double weight = CinematicTimeline.smooth(time / 5) * (1 - CinematicTimeline.smooth((time - 96) / 4));
        for (int i = 0; i < 6; i++) {
            ModelPart part = parts[i];
            double pitch = CinematicTimeline.lerp(POSES[from][i][0], POSES[to][i][0], blend);
            double yaw = CinematicTimeline.lerp(POSES[from][i][1], POSES[to][i][1], blend);
            double roll = CinematicTimeline.lerp(POSES[from][i][2], POSES[to][i][2], blend);
            if (i == 0) {
                double facingWeight = CinematicTimeline.smooth((time-50)/10) * (1-CinematicTimeline.smooth((time-72)/16));
                double trackYaw = MathHelper.clamp(MathHelper.wrapDegrees((float) CinematicTimeline.angle(time)), -65, 65);
                yaw = CinematicTimeline.lerp(yaw, trackYaw, facingWeight);
            }
            part.pitch = (float) CinematicTimeline.lerp(part.pitch, Math.toRadians(pitch), weight);
            part.yaw = (float) CinematicTimeline.lerp(part.yaw, Math.toRadians(yaw), weight);
            part.roll = (float) CinematicTimeline.lerp(part.roll, Math.toRadians(roll), weight);
        }
        // Player model limbs are siblings, not children of the torso. Move their roots with it.
        float bodyYaw = model.body.yaw;
        for (ModelPart arm : new ModelPart[]{model.rightArm, model.leftArm}) {
            float x = arm.pivotX, z = arm.pivotZ;
            arm.pivotX = MathHelper.cos(bodyYaw) * x + MathHelper.sin(bodyYaw) * z;
            arm.pivotZ = -MathHelper.sin(bodyYaw) * x + MathHelper.cos(bodyYaw) * z;
            arm.yaw += bodyYaw;
        }
        model.rightArm.pivotY -= (float) (0.6 * weight * Math.sin(Math.PI * CinematicTimeline.clamp((time-50)/38)));
        model.hat.copyTransform(model.head); model.jacket.copyTransform(model.body);
        model.rightSleeve.copyTransform(model.rightArm); model.leftSleeve.copyTransform(model.leftArm);
        model.rightPants.copyTransform(model.rightLeg); model.leftPants.copyTransform(model.leftLeg);
    }
}
