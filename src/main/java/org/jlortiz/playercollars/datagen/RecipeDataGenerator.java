package org.jlortiz.playercollars.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.DyeColor;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.item.PawsItem;

import java.util.concurrent.CompletableFuture;

public class RecipeDataGenerator extends FabricRecipeProvider {
    public RecipeDataGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlayerCollarsMod.COLLAR_ITEM).pattern(" l ").pattern("lil").pattern(" d ")
                .input('l', Items.LEATHER)
                .input('i', ConventionalItemTags.GOLD_INGOTS)
                .input('d', ConventionalItemTags.DYES)
                .criterion(FabricRecipeProvider.hasItem(Items.LEATHER),
                        FabricRecipeProvider.conditionsFromItem(Items.LEATHER))
                .offerTo(exporter);
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PlayerCollarsMod.CLICKER_ITEM).pattern(" b ").pattern("pip").pattern(" p ")
                .input('b', ItemTags.BUTTONS)
                .input('i', ConventionalItemTags.IRON_INGOTS)
                .input('p', ItemTags.PLANKS)
                .criterion(FabricRecipeProvider.hasItem(Items.IRON_INGOT),
                        FabricRecipeProvider.conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter);
        ShapelessRecipeJsonBuilder.create(RecipeCategory.TOOLS, PlayerCollarsMod.PAW_CONFIGURATION_ITEM)
                .input(ConventionalItemTags.REDSTONE_DUSTS)
                .input(ConventionalItemTags.COPPER_INGOTS)
                .input(PlayerCollarsMod.PAWS_TAG)
                .criterion(FabricRecipeProvider.hasItem(PlayerCollarsMod.PAWS_ITEMS[0]),
                        FabricRecipeProvider.conditionsFromTag(PlayerCollarsMod.PAWS_TAG))
                .offerTo(exporter);
        for (DyeColor c : DyeColor.values())
            generateBed(exporter, PlayerCollarsMod.DOG_BED_ITEMS[c.ordinal()], DatagenEntrypoint.WOOLS[c.ordinal()]);
        for (int i = 0; i < PlayerCollarsMod.PAWS_DYE_COLORS.length; i++)
            generatePaws(exporter, PlayerCollarsMod.PAWS_ITEMS[i], DatagenEntrypoint.WOOLS[PlayerCollarsMod.PAWS_DYE_COLORS[i].ordinal()]);
    }

    private void generateBed(RecipeExporter exporter, BedItem output, Item input) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, output).pattern("w w").pattern("www")
                .input('w', input)
                .criterion(FabricRecipeProvider.hasItem(input),
                        FabricRecipeProvider.conditionsFromItem(input))
                .group("dog_bed")
                .offerTo(exporter);
    }

    private void generatePaws(RecipeExporter exporter, PawsItem output, Item input) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, output).pattern(" w ").pattern("wlw").pattern(" w ")
                .input('w', input)
                .input('l', Items.LEATHER)
                .criterion(FabricRecipeProvider.hasItem(Items.LEATHER),
                        FabricRecipeProvider.conditionsFromItem(Items.LEATHER))
                .group("paws")
                .offerTo(exporter);
    }
}
