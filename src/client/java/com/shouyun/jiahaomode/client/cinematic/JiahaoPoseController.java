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
        double time = JiahaoCinematicController.poseElapsed(player);
        var pose = JiahaoCinematicController.poseType(player);
        double duration = JiahaoCinematicController.poseDuration(player);
        double delay = duration == 60 ? 8 : 0;
        double weight = CinematicTimeline.smooth((time - delay) / 8)
                * (1 - CinematicTimeline.smooth((time - duration + 4) / 4));
        if(JiahaoCinematicController.isBurstPose(player))weight=com.shouyun.jiahaomode.hao.HaoBurstTimeline.poseWeight(time);
        double torsoPitch = Math.toRadians(JiahaoCinematicController.poseDegrees(player,1, 0));
        double torsoYaw = Math.toRadians(JiahaoCinematicController.poseDegrees(player,1, 1));
        for (int i = 0; i < 6; i++) {
            ModelPart part = parts[i];
            boolean upper = i == 0 || i == 2 || i == 3;
            double pitch = Math.toRadians(JiahaoCinematicController.poseDegrees(player,i, 0)) + (upper ? torsoPitch : 0);
            double yaw = Math.toRadians(JiahaoCinematicController.poseDegrees(player,i, 1)) + (upper ? torsoYaw : 0);
            part.pitch = (float) CinematicTimeline.lerp(part.pitch, pitch, weight);
            part.yaw = (float) CinematicTimeline.lerp(part.yaw, yaw, weight);
            part.roll = (float) CinematicTimeline.lerp(part.roll, Math.toRadians(JiahaoCinematicController.poseDegrees(player,i, 2)), weight);
            if (i <= 3) {
                // Upper parts are siblings. Rotate their roots about the hips, not the neck.
                double x = i == 2 ? -5 : i == 3 ? 5 : 0, y = i >= 2 ? 2 : 0;
                double z = (y - 12) * Math.sin(torsoPitch);
                double targetX = x * Math.cos(torsoYaw) + z * Math.sin(torsoYaw);
                double targetZ = -x * Math.sin(torsoYaw) + z * Math.cos(torsoYaw);
                part.pivotX = (float) CinematicTimeline.lerp(part.pivotX, targetX, weight);
                part.pivotY = (float) CinematicTimeline.lerp(part.pivotY, 12 + (y - 12) * Math.cos(torsoPitch), weight);
                part.pivotZ = (float) CinematicTimeline.lerp(part.pivotZ, targetZ, weight);
            }
        }
        model.hat.copyTransform(model.head); model.jacket.copyTransform(model.body);
        model.rightSleeve.copyTransform(model.rightArm); model.leftSleeve.copyTransform(model.leftArm);
        model.rightPants.copyTransform(model.rightLeg); model.leftPants.copyTransform(model.leftLeg);
    }
}
