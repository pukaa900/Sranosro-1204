package nc.rooqata.sranosro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class SranosroMod implements ClientModInitializer {

    // ===== SETTINGS =====
    public static PlacementMode mode = PlacementMode.NEW;
    public static long delayMs = 80;              // placement rate (fps-based)
    public static boolean ignoreUpDown = false;   // OLD mode toggle
    public static int chaosSkipChance = 20;        // CHAOS randomness %

    private static long lastPlaceTime = 0;
    private static final Random rng = new Random();

    @Override
    public void onInitializeClient() {
        System.out.println("[Sranosro] Loaded (client-side only).");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null && client.world != null)
                tick(client);
        });
    }

    // ======================================================
    // MAIN TICK
    // ======================================================
    private void tick(MinecraftClient mc) {

        if (mc.player == null) return;

        // must hold a block
        ItemStack held = mc.player.getMainHandStack();
        if (!(held.getItem() instanceof BlockItem)) return;

        // stop when GUI open
        if (mc.currentScreen != null) return;

        switch (mode) {
            case OLD -> doOld(mc);
            case NEW -> doNew(mc);
            case CHAOS -> doChaos(mc);
        }
    }

    // ======================================================
    // OLD MODE (vanilla-style interact)
    // ======================================================
    private void doOld(MinecraftClient mc) {

        BlockHitResult hit = getHit(mc);
        if (hit == null) return;

        if (ignoreUpDown) {
            Direction f = hit.getSide();
            if (f == Direction.UP || f == Direction.DOWN) return;
        }

        if (!ready()) return;
        lastPlaceTime = now();

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ======================================================
    // NEW MODE (precise face placement)
    // ======================================================
    private void doNew(MinecraftClient mc) {

        BlockHitResult hit = getHit(mc);
        if (hit == null) return;

        BlockPos placePos = hit.getBlockPos().offset(hit.getSide());

        if (!ready()) return;
        lastPlaceTime = now();

        placeDirect(mc, placePos);
    }

    // ======================================================
    // CHAOS MODE (random ruin builder)
    // ======================================================
    private void doChaos(MinecraftClient mc) {

        BlockHitResult hit = getHit(mc);
        if (hit == null) return;

        if (rng.nextInt(100) < chaosSkipChance) return;

        Direction randFace = Direction.values()[rng.nextInt(Direction.values().length)];
        int distance = rng.nextBoolean() ? 1 : 0;

        BlockPos placePos = hit.getBlockPos().offset(randFace, distance);

        if (!ready()) return;
        lastPlaceTime = now();

        placeDirect(mc, placePos);
    }

    // ======================================================
    // HELPERS
    // ======================================================
    private BlockHitResult getHit(MinecraftClient mc) {
        if (mc.crosshairTarget == null) return null;
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return null;
        return (BlockHitResult) mc.crosshairTarget;
    }

    private boolean ready() {
        return now() - lastPlaceTime >= delayMs;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void placeDirect(MinecraftClient mc, BlockPos pos) {

        ItemStack held = mc.player.getMainHandStack();
        if (!(held.getItem() instanceof BlockItem item)) return;

        BlockState state = item.getBlock().getDefaultState();

        if (!mc.world.getBlockState(pos).isAir()) return;

        mc.world.setBlockState(pos, state);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
