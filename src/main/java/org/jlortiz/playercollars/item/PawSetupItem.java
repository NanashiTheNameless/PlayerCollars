package org.jlortiz.playercollars.item;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jlortiz.playercollars.PlayerCollarsMod;

public class PawSetupItem extends Item {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "paws_unbound"));

    public PawSetupItem() {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY));
    }
}
