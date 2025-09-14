package org.jlortiz.playercollars;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.jlortiz.playercollars.item.ClickerItem;
import org.jlortiz.playercollars.item.CollarItem;
import org.jlortiz.playercollars.leash.LeashImpl;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class PlayerCollarsMod implements ModInitializer {
	public static final String MOD_ID = "playercollars";
	public static final CollarItem COLLAR_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "collar"), new CollarItem());
	public static final ClickerItem CLICKER_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "clicker"), new ClickerItem());
	public static final SoundEvent CLICKER_ON = Registry.register(Registries.SOUND_EVENT, new Identifier(MOD_ID, "clicker_on"),
			SoundEvent.of(new Identifier(MOD_ID, "clicker_on")));
	public static final SoundEvent CLICKER_OFF = Registry.register(Registries.SOUND_EVENT, new Identifier(MOD_ID, "clicker_off"),
			SoundEvent.of(new Identifier(MOD_ID, "clicker_off")));

	public static final EntityAttribute ATTR_LEASH_DISTANCE = Registry.register(
			Registries.ATTRIBUTE, Identifier.of(PlayerCollarsMod.MOD_ID, "leash_distance"),
			new ClampedEntityAttribute("attribute.playercollars.leash_distance", 4, 2, 16));
	public static final GameRules.Key<GameRules.BooleanRule> PLAYER_LEASHES_BREAK_RULE = GameRuleRegistry.register(
			"playerLeashesBreak", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	public static ItemStack filterStacksByOwner(List<Pair<SlotReference, ItemStack>> stacks, UUID plr) {
		for (Pair<SlotReference, ItemStack> p : stacks) {
			ItemStack is = p.getRight();
			if (is.getItem() instanceof CollarItem item) {
				Pair<UUID, String> owner = item.getOwner(is);
				if (owner != null && owner.getLeft().equals(plr)) {
					return is;
				}
			}
		}
		return null;
	}

	public static ActionResult pullPlayerTowards(ServerPlayerEntity plr, Vec3d towards, double minDist, double maxDist, UnaryOperator<Double> getFactor) {
		Vec3d vecTo = towards.subtract(plr.getPos());
		double distance = vecTo.length();
		if (distance < minDist) return ActionResult.PASS;
		if (distance > maxDist) return ActionResult.FAIL;

		plr.addVelocity(vecTo.multiply(Math.abs(getFactor.apply(distance))));
		plr.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(plr));
		plr.velocityDirty = false;
		return ActionResult.SUCCESS;
	}

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "update_collar"), PacketUpdateCollar::handle);
		TrinketsApi.registerTrinket(PlayerCollarsMod.COLLAR_ITEM, PlayerCollarsMod.COLLAR_ITEM);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(itemGroup -> {
			itemGroup.add(COLLAR_ITEM);
			itemGroup.add(CLICKER_ITEM);
		});

		PlayerBlockBreakEvents.BEFORE.register((World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) -> {
			if (world.isClient) return true;
			if (player.isSpectator()) return true;
			net.minecraft.entity.Entity leashHolderEntity = ((LeashImpl) player).leashplayers$getProxyLeashHolder();
			if (leashHolderEntity instanceof LeashKnotEntity knot && pos.equals(knot.getBlockPos())) {
				player.sendMessage(Text.translatable("message.playercollars.no_break_fence").formatted(Formatting.RED), true);
				return false;
			}
			return true;
		});

		AttackEntityCallback.EVENT.register((PlayerEntity player, World var2, Hand var3, Entity var4, @Nullable EntityHitResult var5) -> {
			if (var2.isClient) return ActionResult.PASS;
			if (player.isSpectator()) return ActionResult.PASS;
			if (var4 instanceof PlayerEntity &&
					TrinketsApi.getTrinketComponent(player).map((x) -> x.getEquipped(PlayerCollarsMod.COLLAR_ITEM))
							.map((x) -> PlayerCollarsMod.filterStacksByOwner(x, var4.getUuid())).isPresent()) {
				// Collared players are allowed to attack bad owners, but have 75% damage returned to them
				player.sendMessage(Text.translatable("message.playercollars.no_attack_owner").formatted(Formatting.RED), true);
				double f = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
				f = (f - 1) * 0.75 + 1;
				player.damage(player.getDamageSources().playerAttack(player), (float) Math.ceil(f));
				return ActionResult.PASS;
			}

			Entity leashedEnt = ((LeashImpl) player).leashplayers$getProxyLeashHolder();
			if (leashedEnt instanceof LeashKnotEntity && leashedEnt.equals(var4)) {
				player.sendMessage(Text.translatable("message.playercollars.no_break_fence").formatted(Formatting.RED), true);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
	}
}
