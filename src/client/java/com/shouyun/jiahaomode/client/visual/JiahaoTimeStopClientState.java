// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client.visual;

import com.shouyun.jiahaomode.client.mixin.WorldRendererTimeAccess;
import com.shouyun.jiahaomode.timestop.JiahaoTimeStopManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import java.lang.ref.WeakReference;

/** Visual snapshot of the existing server-authoritative state, owned by the current ClientWorld. */
public final class JiahaoTimeStopClientState {
	private static final FrozenRenderClock CLOCK = new FrozenRenderClock();
	private static WeakReference<ClientWorld> currentWorld = new WeakReference<>(null);
	private static double lastFrameTime = Double.NaN;
	private static float lastFrameDelta;
	private static float frozenFrameDelta;
	private static float rain;
	private static float thunder;

	private JiahaoTimeStopClientState() { }
	public static void onWorldChanged(ClientWorld world) {
		if (currentWorld.get() == world) return;
		com.shouyun.jiahaomode.client.cinematic.JiahaoCinematicController.cleanup();
		currentWorld = new WeakReference<>(world);
		CLOCK.reset(); lastFrameTime = Double.NaN; lastFrameDelta = 0; frozenFrameDelta = 0;
	}
	public static void onStateSync(ClientWorld world, boolean active) {
		onWorldChanged(world);
		if (active && !CLOCK.isFrozen()) {
			float delta = Double.isNaN(lastFrameTime) ? MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false) : lastFrameDelta;
			rain = world.getRainGradient(delta);
			thunder = ((JiahaoWeatherAccess) world).jiahao$rawThunder(delta);
			frozenFrameDelta = delta;
			CLOCK.freeze(Double.isNaN(lastFrameTime) ? rawTime() : lastFrameTime);
			((JiahaoWeatherAccess) world).jiahao$setWeatherLevels(rain, thunder);
		} else if (!active && CLOCK.isFrozen()) {
			CLOCK.release(rawTime());
			((JiahaoWeatherAccess) world).jiahao$setWeatherLevels(rain, thunder);
		}
	}
	private static double rawTime() {
		MinecraftClient client = MinecraftClient.getInstance();
		return (double) ((WorldRendererTimeAccess) client.worldRenderer).jiahao$getRendererTicks() + client.getRenderTickCounter().getTickDelta(false);
	}
	public static boolean isTimeStopped(World world) {
		return world != null && world == currentWorld.get() && CLOCK.isFrozen() && JiahaoTimeStopManager.isTimeStopped(world);
	}
	public static boolean hasAnimationOffset(World world) { return world != null && world == currentWorld.get() && CLOCK.hasAdjustment(); }
	public static FrozenRenderClock.Phase renderPhase(ClientWorld world, int ticks, float delta) {
		onWorldChanged(world);
		lastFrameTime = (double) ticks + delta; lastFrameDelta = delta;
		return CLOCK.phase(ticks, delta);
	}
	public static float frozenTickDelta() { return frozenFrameDelta; }
	public static double getWorldAnimationTime() { return CLOCK.sample(rawTime()); }
	public static float frozenRain() { return rain; }
	public static float frozenThunder() { return thunder * rain; }
	public static boolean shouldFreezeParticle(World world, Particle particle) {
		return isTimeStopped(world) && !((JiahaoTimeStopParticle) particle).jiahao$isTimeStopImmune();
	}
	public static <T extends Particle> T markTimeStopImmune(T particle) {
		((JiahaoTimeStopParticle) particle).jiahao$setTimeStopImmune(true);
		return particle;
	}
}
