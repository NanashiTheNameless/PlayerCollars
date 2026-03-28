package io.github.nanashithenameless.playercollars.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import io.github.nanashithenameless.playercollars.OwnerComponent;
import io.github.nanashithenameless.playercollars.PlayerCollarsMod;

import java.util.List;

public class OwnershipCraftingRecipe extends SpecialCraftingRecipe {
    private IngredientPlacement ingredientPlacement;
    private final Ingredient base;

    public OwnershipCraftingRecipe(CraftingRecipeCategory category, Ingredient base) {
        super(category);
        this.base = base;
    }

    public boolean matches(CraftingRecipeInput craftingRecipeInput, World world) {
        if (!craftingRecipeInput.getRecipeMatcher().isCraftable(this, null)) return false;

        for (int i = 0; i < craftingRecipeInput.size(); i++) {
            ItemStack is = craftingRecipeInput.getStackInSlot(i);
            if (base.test(is) && is.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE) != null) return false;
        }
        return true;
    }

    public ItemStack craft(CraftingRecipeInput craftingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
        ItemStack output = ItemStack.EMPTY;
        OwnerComponent owner = null;

        for(int j = 0; j < craftingRecipeInput.size(); j++) {
            ItemStack is = craftingRecipeInput.getStackInSlot(j);
            if (!is.isEmpty()) {
                if (is.isOf(PlayerCollarsMod.DEED_OF_OWNERSHIP_STAMPED)) {
                    owner = is.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
                } else if (base.test(is)) {
                    output = is.copy();
                }
            }
        }

        if (owner == null || output.isEmpty()) return ItemStack.EMPTY;
        output.set(PlayerCollarsMod.OWNER_COMPONENT_TYPE, owner);
        return output;
    }

    private Ingredient getBase() {
        return base;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        if (ingredientPlacement == null) {
            ingredientPlacement = IngredientPlacement.forShapeless(List.of(base, Ingredient.ofItem(PlayerCollarsMod.DEED_OF_OWNERSHIP_STAMPED)));
        }

        return ingredientPlacement;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<OwnershipCraftingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<OwnershipCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder.group(
            CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(CraftingRecipe::getCategory),
            Ingredient.CODEC.fieldOf("base").forGetter(OwnershipCraftingRecipe::getBase)
        ).apply(builder, OwnershipCraftingRecipe::new));
        public static final PacketCodec<RegistryByteBuf, OwnershipCraftingRecipe> PACKET_CODEC = PacketCodec.tuple(
                CraftingRecipeCategory.PACKET_CODEC, CraftingRecipe::getCategory,
                Ingredient.PACKET_CODEC, OwnershipCraftingRecipe::getBase, OwnershipCraftingRecipe::new
        );

        public MapCodec<OwnershipCraftingRecipe> codec() {
            return CODEC;
        }

        public PacketCodec<RegistryByteBuf, OwnershipCraftingRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
