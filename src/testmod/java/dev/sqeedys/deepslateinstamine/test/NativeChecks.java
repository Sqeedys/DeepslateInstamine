package dev.sqeedys.deepslateinstamine.test;

import com.mojang.authlib.GameProfile;
import dev.sqeedys.deepslateinstamine.DeepslateInstamine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Native progress checks only: no claims about real client prediction or break/drop events. */
public final class NativeChecks implements ModInitializer {
    private final List<String> passed = new ArrayList<>();
    private TestPlayer player;
    private static final BlockPos POS = new BlockPos(0, 80, 0);

    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            String phase = System.getProperty("deepslate.test.phase", "default");
            Throwable failure = null;
            try {
                if (!Set.of("default", "optin").contains(phase)) throw new IllegalArgumentException("Unknown test phase");
                run(server, phase);
            } catch (Throwable problem) { failure = problem; problem.printStackTrace(); }
            try {
                Path receipt = FabricLoader.getInstance().getGameDir().resolve("local/deepslate-integration-" + phase + ".txt");
                Files.createDirectories(receipt.getParent());
                String result = (failure == null ? "PASS" : "FAIL") + " phase=" + phase + " checks=" + passed.size()
                    + "\n" + String.join("\n", passed) + "\n" + (failure == null ? "" : failure.toString());
                Files.writeString(receipt, result);
                System.out.println(result);
            } catch (Exception error) { throw new RuntimeException("Cannot save test receipt", error); }
            finally { server.halt(false); }
        });
    }

    private void run(MinecraftServer server, String phase) {
        Set<Block> tagged = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().is(DeepslateInstamine.INSTANT_MINEABLE_DEEPSLATE)) tagged.add(block);
        }
        Set<Block> expected = phase.equals("default") ? Set.of(Blocks.DEEPSLATE)
            : Set.of(Blocks.DEEPSLATE, Blocks.STONE, Blocks.BEDROCK);
        check(tagged.equals(expected), "exact " + phase + " block tag contents");
        player = new TestPlayer(server);
        equip(Items.NETHERITE_PICKAXE, 5, 1);
        float normal = progress(Blocks.STONE);
        equal(progress(Blocks.DEEPSLATE), normal, "qualified deepslate matches native stone");
        check(normal >= 1.0F, "Efficiency V Haste II reaches one-tick progress");
        equip(Items.NETHERITE_PICKAXE, 5, 2);
        equal(progress(Blocks.DEEPSLATE), progress(Blocks.STONE), "Haste III also qualifies");
        check(progress(Blocks.DEEPSLATE) >= 1.0F, "higher Haste reaches one-tick progress");
        equip(Items.DIAMOND_PICKAXE, 5, 1);
        unchanged("diamond pickaxe is excluded");
        equip(Items.NETHERITE_AXE, 5, 1);
        unchanged("wrong tool is excluded");
        equip(Items.NETHERITE_PICKAXE, 5, -1);
        unchanged("missing Haste preserves vanilla progress");
        equip(Items.NETHERITE_PICKAXE, 5, 0);
        unchanged("Haste I preserves vanilla progress");
        equip(Items.NETHERITE_PICKAXE, 4, 1);
        unchanged("Efficiency IV preserves vanilla progress");
        equip(Items.NETHERITE_PICKAXE, 0, 1);
        unchanged("missing Efficiency preserves vanilla progress");
        equip(Items.NETHERITE_PICKAXE, 5, 1);
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 1200, 0));
        equal(progress(Blocks.DEEPSLATE), progress(Blocks.STONE), "Mining Fatigue retains stone-equivalent penalty");
        check(progress(Blocks.DEEPSLATE) < normal && progress(Blocks.DEEPSLATE) < 1, "Mining Fatigue prevents instant progress");
        equip(Items.NETHERITE_PICKAXE, 5, 1);
        player.setOnGround(false);
        equal(progress(Blocks.DEEPSLATE), progress(Blocks.STONE), "airborne penalty retained");
        check(progress(Blocks.DEEPSLATE) < normal && progress(Blocks.DEEPSLATE) < 1, "airborne mining is slower");
        equip(Items.NETHERITE_PICKAXE, 5, 1);
        player.eyesInWater = true;
        equal(progress(Blocks.DEEPSLATE), progress(Blocks.STONE), "native submerged mining penalty retained");
        check(progress(Blocks.DEEPSLATE) < normal && progress(Blocks.DEEPSLATE) < 1, "submerged mining is slower");
        equip(Items.NETHERITE_PICKAXE, 5, 1);
        float vanillaCobbled = player.getDestroySpeed(Blocks.COBBLED_DEEPSLATE.defaultBlockState())
            / Blocks.COBBLED_DEEPSLATE.defaultBlockState().getDestroySpeed(player.level(), POS) / 30.0F;
        equal(progress(Blocks.COBBLED_DEEPSLATE), vanillaCobbled, "untagged cobbled deepslate remains vanilla");
        equal(progress(Blocks.STONE), normal, "stone reference is stable without recursion");
        equal(progress(Blocks.BEDROCK), 0, "bedrock remains unbreakable even when opted in");
    }

    private void equip(Item item, int efficiency, int hasteAmplifier) {
        player.eyesInWater = false;
        player.removeAllEffects();
        ItemStack stack = new ItemStack(item);
        if (efficiency > 0) stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.EFFICIENCY), efficiency);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        player.updateEquipment();
        if (hasteAmplifier >= 0) player.addEffect(new MobEffectInstance(MobEffects.HASTE, 1200, hasteAmplifier));
        player.setOnGround(true);
    }

    private float progress(Block block) { return block.defaultBlockState().getDestroyProgress(player, player.level(), POS); }
    private void unchanged(String name) { equal(progress(Blocks.DEEPSLATE), progress(Blocks.STONE) / 2.0F, name); }
    private void equal(float actual, float expected, String name) {
        check(Float.isFinite(actual) && Math.abs(actual - expected) <= Math.max(0.000001F, Math.abs(expected) * 0.00001F),
            name + " actual=" + actual + " expected=" + expected);
    }
    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed.add(name);
    }

    private static final class TestPlayer extends ServerPlayer {
        private boolean eyesInWater;
        private final Map<EquipmentSlot, ItemStack> previousEquipment = new EnumMap<>(EquipmentSlot.class);
        // Supply only the environment predicate; Player.getDestroySpeed computes the native water penalty.
        @Override public boolean isEyeInFluid(TagKey<Fluid> tag) { return eyesInWater && tag.equals(FluidTags.WATER); }
        TestPlayer(MinecraftServer server) {
            super(server, server.overworld(), new GameProfile(UUID.fromString("00000000-0000-4000-8000-000000000263"), "MiningFixture"), ClientInformation.createDefault());
            for (EquipmentSlot slot : EquipmentSlot.VALUES) previousEquipment.put(slot, ItemStack.EMPTY);
            connection = new ServerGamePacketListenerImpl(server, new net.minecraft.network.Connection(PacketFlow.SERVERBOUND) {}, this,
                CommonListenerCookie.createInitial(getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { }
                @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener) { }
            };
        }
        void updateEquipment() {
            // The native equipment-diff method applies/removes enchantment attribute effects.
            collectEquipmentChanges(previousEquipment);
            for (EquipmentSlot slot : EquipmentSlot.VALUES) previousEquipment.put(slot, getItemBySlot(slot).copy());
        }
    }
}
