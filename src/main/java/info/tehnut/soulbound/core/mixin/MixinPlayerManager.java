package info.tehnut.soulbound.core.mixin;

import info.tehnut.soulbound.core.SlottedItem;
import info.tehnut.soulbound.Soulbound;
import info.tehnut.soulbound.api.SoulboundContainer;
import info.tehnut.soulbound.core.SoulboundPersistentState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setMainHand(Lnet/minecraft/util/AbsoluteHand;)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void enchants$respawnPlayer(ServerPlayerEntity oldPlayer, DimensionType dimension, boolean dimensionChange, CallbackInfoReturnable<ServerPlayerEntity> callback, BlockPos pos, boolean forcedSpawn, ServerPlayerInteractionManager interactionManager, ServerPlayerEntity newPlayer) {
        if (dimensionChange)
            return;

        SoulboundPersistentState persistentState = server.getWorld(DimensionType.OVERWORLD).getPersistentStateManager().getOrCreate(SoulboundPersistentState::new, "soulbound_persisted_items");

        List<SlottedItem> savedItems = persistentState.restorePlayer(oldPlayer);
        if (savedItems == null)
            return;

        SoulboundContainer.CONTAINERS.forEach((id, container) -> {
            List<ItemStack> newInventory = container.getContainerStacks(newPlayer);
            savedItems.stream().filter(item -> item.getContainerId().equals(id)).forEach(item -> {
                if (newPlayer.getRand().nextFloat() <= Soulbound.CONFIG.get().getSoulboundRemovalChance()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item.getStack());
                    enchantments.remove(Soulbound.ENCHANT_SOULBOUND);
                    EnchantmentHelper.set(enchantments, item.getStack());
                }

                newInventory.set(item.getSlot(), item.getStack());
            });
        });

        savedItems.clear();
    }
}
