package org.jlortiz.playercollars;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.jlortiz.playercollars.item.*;
import org.jlortiz.playercollars.leash.LeashImpl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerCollarsMod implements ModInitializer {
	public static final String MOD_ID = "playercollars";
	public static final CollarItem COLLAR_ITEM = Registry.register(Registries.ITEM, CollarItem.REGISTRY_KEY, new CollarItem());
	public static final ClickerItem CLICKER_ITEM = Registry.register(Registries.ITEM, ClickerItem.REGISTRY_KEY, new ClickerItem());
	public static final PawsItem PAWS_ITEM = Registry.register(Registries.ITEM, PawsItem.REGISTRY_KEY, new PawsItem());
	public static final PawSetupItem PAW_CONFIGURATION_ITEM = Registry.register(Registries.ITEM, PawSetupItem.REGISTRY_KEY, new PawSetupItem());
	public static final SoundEvent CLICKER_ON = Registry.register(Registries.SOUND_EVENT, Identifier.of(MOD_ID, "clicker_on"),
			SoundEvent.of(Identifier.of(MOD_ID, "clicker_on")));
	public static final SoundEvent CLICKER_OFF = Registry.register(Registries.SOUND_EVENT, Identifier.of(MOD_ID, "clicker_off"),
			SoundEvent.of(Identifier.of(MOD_ID, "clicker_off")));

	private static final Codec<OwnerComponent> OWNER_COMPONENT_CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Uuids.CODEC.fieldOf("uuid").forGetter(OwnerComponent::uuid),
            Codec.STRING.fieldOf("name").forGetter(OwnerComponent::name)
    ).apply(builder, OwnerComponent::new));
	public static final ComponentType<OwnerComponent> OWNER_COMPONENT_TYPE = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			Identifier.of(MOD_ID, "owner_component"),
			ComponentType.<OwnerComponent>builder().codec(OWNER_COMPONENT_CODEC).build());

	private static final Codec<Set<Identifier>> CAN_INTERACT_COMPONENT_CODEC = new ListCodec<>(
			Identifier.CODEC, 0, 65535).xmap(Set::copyOf, List::copyOf);
	public static final ComponentType<Set<Identifier>> CAN_INTERACT_COMPONENT_TYPE = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			Identifier.of(MOD_ID, "can_interact_component_component"),
			ComponentType.<Set<Identifier>>builder().codec(CAN_INTERACT_COMPONENT_CODEC).build());

	public static final RegistryEntry<EntityAttribute> ATTR_CLICKER_DISTANCE = Registry.registerReference(
			Registries.ATTRIBUTE, Identifier.of(PlayerCollarsMod.MOD_ID, "clicker_distance"),
			new ClampedEntityAttribute("attribute.playercollars.clicker_distance", 4, 0, 32));
	public static final RegistryEntry<EntityAttribute> ATTR_LEASH_DISTANCE = Registry.registerReference(
			Registries.ATTRIBUTE, Identifier.of(PlayerCollarsMod.MOD_ID, "leash_distance"),
			new ClampedEntityAttribute("attribute.playercollars.leash_distance", 4, 2, 4));

	public static final DogBedBlock[] DOG_BEDS = new DogBedBlock[DyeColor.values().length];
	public static final BedItem[] DOG_BED_ITEMS = new BedItem[DyeColor.values().length];
	public static final TagKey<Block> PAWS_ALLOW_INTERACT = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "paws_allow_interact"));

	public static ItemStack filterStacksByOwner(Iterable<SlotEntryReference> stacks, UUID plr) {
		for (SlotEntryReference p : stacks) {
			ItemStack is = p.stack();
			OwnerComponent owner = is.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
			if (owner != null && owner.uuid().equals(plr)) {
				return is;
			}
		}
		return null;
	}

	@Override
	public void onInitialize() {
		Registry.register(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Identifier.of(PlayerCollarsMod.MOD_ID, "regeneration_effect"), RegenerationEnchantmentEffect.CODEC);
		PayloadTypeRegistry.playC2S().register(PacketUpdateCollar.ID, PacketUpdateCollar.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PacketUpdateCollar.ID, PacketUpdateCollar::handle);
		PayloadTypeRegistry.playS2C().register(PacketLookAtLerped.ID, PacketLookAtLerped.CODEC);
		AccessoryRegistry.register(COLLAR_ITEM, COLLAR_ITEM);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(itemGroup -> {
			itemGroup.add(COLLAR_ITEM);
			itemGroup.add(CLICKER_ITEM);
			itemGroup.add(PAWS_ITEM);
			itemGroup.add(PAW_CONFIGURATION_ITEM);
		});

		for (DyeColor c : DyeColor.values()) {
			RegistryKey<Block> blockKey = DogBedBlock.getRegistryKey(c);
			DOG_BEDS[c.ordinal()] = Registry.register(Registries.BLOCK, blockKey, new DogBedBlock(c, blockKey));
			RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, blockKey.getValue());
			DOG_BED_ITEMS[c.ordinal()] = Registry.register(Registries.ITEM, itemKey,
					new BedItem(DOG_BEDS[c.ordinal()], (new Item.Settings()).maxCount(1).registryKey(itemKey)));
		}
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(itemGroup -> {
			for (BedItem bed : DOG_BED_ITEMS)
				itemGroup.add(bed);
		});

		PlayerBlockBreakEvents.BEFORE.register((World var1, PlayerEntity player, BlockPos blockPos, BlockState var4, @Nullable BlockEntity var5) -> {
			if (var1.isClient) return true;
			if (player.isSpectator()) return true;
			Entity leashHolderEntity = ((LeashImpl) player).leashplayers$getProxyLeashHolder();
			if (leashHolderEntity instanceof LeashKnotEntity knot && blockPos.equals(knot.getAttachedBlockPos())) {
				player.sendMessage(Text.translatable("message.playercollars.no_break_fence").formatted(Formatting.RED), true);
				return false;
			}
			return true;
		});

		AttackEntityCallback.EVENT.register((PlayerEntity player, World var2, Hand var3, Entity var4, @Nullable EntityHitResult var5) -> {
			if (var2.isClient) return ActionResult.PASS;
			if (player.isSpectator()) return ActionResult.PASS;
			AccessoriesCapability cap = AccessoriesCapability.get(player);
			if (cap != null) {
				for (SlotEntryReference sr : cap.getEquipped(PlayerCollarsMod.COLLAR_ITEM)) {
					OwnerComponent owner = sr.stack().get(OWNER_COMPONENT_TYPE);
					if (owner != null && owner.uuid().equals(var4.getUuid())) {
						// Collared players are allowed to attack bad owners, but have 75% damage returned to them
						player.sendMessage(Text.translatable("message.playercollars.no_attack_owner").formatted(Formatting.RED), true);
						double f = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
						f = (f - 1) * 0.75 + 1;
						player.damage((ServerWorld) var2, player.getDamageSources().playerAttack(player), (float) Math.ceil(f));
						return ActionResult.PASS;
					}
				}
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
