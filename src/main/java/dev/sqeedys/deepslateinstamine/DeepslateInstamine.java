package dev.sqeedys.deepslateinstamine;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class DeepslateInstamine {
    public static final String MOD_ID = "deepslate_instamine";
    public static final TagKey<Block> INSTANT_MINEABLE_DEEPSLATE = TagKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(MOD_ID, "instant_mineable_deepslate")
    );

    private DeepslateInstamine() {}
}
