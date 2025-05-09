package org.jlortiz.playercollars.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.DyeColor;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.concurrent.CompletableFuture;

public class RecipeDataGenerator extends FabricRecipeProvider {
    public RecipeDataGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {
                createShaped(RecipeCategory.MISC, PlayerCollarsMod.COLLAR_ITEM).pattern(" l ").pattern("lil").pattern(" d ")
                        .input('l', Items.LEATHER)
                        .input('i', ConventionalItemTags.GOLD_INGOTS)
                        .input('d', ConventionalItemTags.DYES)
                        .criterion(hasItem(Items.LEATHER), conditionsFromItem(Items.LEATHER))
                        .offerTo(exporter);
                createShaped(RecipeCategory.MISC, PlayerCollarsMod.CLICKER_ITEM).pattern(" b ").pattern("pip").pattern(" p ")
                        .input('b', ItemTags.BUTTONS)
                        .input('i', ConventionalItemTags.IRON_INGOTS)
                        .input('p', ItemTags.PLANKS)
                        .criterion(hasItem(Items.IRON_INGOT), conditionsFromTag(ConventionalItemTags.IRON_INGOTS))
                        .offerTo(exporter);
                createShaped(RecipeCategory.MISC, PlayerCollarsMod.PAWS_ITEM).pattern(" w ").pattern("wlw").pattern(" w ")
                        .input('w', ItemTags.WOOL)
                        .input('l', Items.LEATHER)
                        .criterion(hasItem(Items.LEATHER), conditionsFromItem(Items.LEATHER))
                        .offerTo(exporter);
                createShapeless(RecipeCategory.TOOLS, PlayerCollarsMod.PAW_CONFIGURATION_ITEM)
                        .input(ConventionalItemTags.REDSTONE_DUSTS)
                        .input(ConventionalItemTags.COPPER_INGOTS)
                        .input(PlayerCollarsMod.PAWS_ITEM)
                        .criterion(hasItem(PlayerCollarsMod.PAWS_ITEM), conditionsFromItem(PlayerCollarsMod.PAWS_ITEM))
                        .offerTo(exporter);
                for (DyeColor c : DyeColor.values())
                    generateBed(exporter, PlayerCollarsMod.DOG_BED_ITEMS[c.ordinal()], DatagenEntrypoint.WOOLS[c.ordinal()]);
            }

            private void generateBed(RecipeExporter exporter, BedItem output, Item input) {
                createShaped(RecipeCategory.DECORATIONS, output).pattern("w w").pattern("www")
                        .input('w', input)
                        .criterion(hasItem(input), conditionsFromItem(input))
                        .group("dog_bed")
                        .offerTo(exporter);
            }
        };
    }

    @Override
    public String getName() {
        return PlayerCollarsMod.MOD_ID + "_recipe_generator";
    }
}
