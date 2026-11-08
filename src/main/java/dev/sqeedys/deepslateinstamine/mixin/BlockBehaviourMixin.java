package dev.sqeedys.deepslateinstamine.mixin;

import dev.sqeedys.deepslateinstamine.DeepslateInstamine;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Changes only the final progress value for explicitly tagged deepslate after
 * vanilla has already evaluated the player and block. The actual break remains
 * entirely in vanilla's normal client/server block-breaking pipeline.
 */
@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourMixin {
    private static final int MIN_EFFICIENCY_LEVEL = 5;
    private static final int MIN_HASTE_LEVEL = 2;

    @Inject(
        method = "getDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void deepslateInstamine$useStoneProgress(
        BlockState state,
        Player player,
        BlockGetter level,
        net.minecraft.core.BlockPos pos,
        CallbackInfoReturnable<Float> callback
    ) {
        // Stone is the reference calculation, even if a datapack tags it.
        // Never override an unbreakable/otherwise zero-progress result.
        if (state.is(Blocks.STONE) || callback.getReturnValueF() <= 0.0F
            || !state.is(DeepslateInstamine.INSTANT_MINEABLE_DEEPSLATE)
            || !player.getMainHandItem().is(Items.NETHERITE_PICKAXE)
            || !player.hasCorrectToolForDrops(state)
            || !hasRequiredHaste(player)
            || !hasRequiredEfficiency(player)) {
            return;
        }

        // Re-evaluate vanilla stone progress instead of reproducing its speed formula.
        // This retains Mining Fatigue, water/air penalties,
        // attribute effects, and any other vanilla player-side speed adjustments.
        callback.setReturnValue(Blocks.STONE.defaultBlockState().getDestroyProgress(player, level, pos));
    }

    private static boolean hasRequiredHaste(Player player) {
        MobEffectInstance haste = player.getEffect(MobEffects.HASTE);
        return haste != null && haste.getAmplifier() + 1 >= MIN_HASTE_LEVEL;
    }

    private static boolean hasRequiredEfficiency(Player player) {
        Holder<Enchantment> efficiency = player.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.EFFICIENCY);
        ItemStack tool = player.getMainHandItem();
        return EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool) >= MIN_EFFICIENCY_LEVEL;
    }
}
