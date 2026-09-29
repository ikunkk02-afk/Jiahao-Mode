// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class JiahaoCinematicCamera {
    private static Frame entry;
    private static double safeDistance = Double.NaN, lastFrame;
    private JiahaoCinematicCamera() { }
    public record Frame(Vec3d position, float yaw, float pitch) { }
    public static void reset() { entry = null; safeDistance = Double.NaN; lastFrame = 0; }
    public static Vec3d orbit(double ticks) {
        double facing = Math.toRadians(JiahaoCinematicController.yaw());
        Vec3d front = new Vec3d(-Math.sin(facing), 0, Math.cos(facing));
        Vec3d right = new Vec3d(-Math.cos(facing), 0, -Math.sin(facing));
        double angle = Math.toRadians(CinematicTimeline.angle(ticks)), radius = CinematicTimeline.radius(ticks);
        return JiahaoCinematicController.origin().add(front.multiply(radius * Math.cos(angle)))
                .add(right.multiply(radius * Math.sin(angle))).add(0, CinematicTimeline.height(ticks), 0);
    }
    public static Frame sample(Camera vanilla) {
        Frame normal = new Frame(vanilla.getPos(), vanilla.getYaw(), vanilla.getPitch());
        if (entry == null) entry = normal;
        double ticks = JiahaoCinematicController.elapsedTicks(), weight = JiahaoCinematicController.cameraWeight();
        Vec3d target = JiahaoCinematicController.origin().add(0, 1.45, 0);
        Vec3d desired = orbit(ticks);
        // Interpolate from the captured entry, or towards the current vanilla exit camera.
        Frame base = ticks < 7 && !JiahaoCinematicController.isReturning() ? entry : normal;
        desired = base.position().lerp(desired, weight);
        desired = collide(target, desired);
        Vec3d look = target.subtract(desired);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        // During the orbit use strict LookAt; only entry/exit blend into normal control.
        return new Frame(desired, MathHelper.lerpAngleDegrees((float) weight, base.yaw(), yaw),
                MathHelper.lerp((float) weight, base.pitch(), pitch));
    }
    public static Vec3d collide(Vec3d target, Vec3d desired) {
        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return desired;
        Vec3d path = desired.subtract(target);
        double length = path.length();
        if (length < 0.00001) return target;
        double permitted = length;
        // Eight offset rays keep a small camera volume (including its near plane) clear.
        for (int i = 0; i < 8; i++) {
            Vec3d offset = new Vec3d((i & 1) == 0 ? -.12 : .12, (i & 2) == 0 ? -.12 : .12, (i & 4) == 0 ? -.12 : .12);
            Vec3d start = target.add(offset);
            var hit = client.world.raycast(new RaycastContext(start, desired.add(offset),
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS) permitted = Math.min(permitted, Math.max(0, start.distanceTo(hit.getPos()) - .15));
        }
        double frame = JiahaoCinematicController.rawFrame();
        double dt = Math.max(0, Math.min(1, (frame - lastFrame) / 20)); lastFrame = frame;
        if (Double.isNaN(safeDistance) || permitted < safeDistance) safeDistance = permitted;
        else safeDistance += (permitted - safeDistance) * (1 - Math.exp(-12 * dt));
        // Never smooth through an obstacle, and never impose an unsafe minimum distance.
        return target.add(path.multiply(Math.min(permitted, safeDistance) / length));
    }
}
