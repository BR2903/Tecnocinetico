package com.bryanamaya.tecnocinetico.item;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.custom.MagazineLoadingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Serializadores de recetas especiales del mod (las normales son JSON en data/tecnocinetico/recipes). */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Tecnocinetico.MOD_ID);

    /** Cargador + balas → cargador con balas (data/tecnocinetico/recipes/magazine_loading.json). */
    public static final RegistryObject<RecipeSerializer<MagazineLoadingRecipe>> MAGAZINE_LOADING =
            SERIALIZERS.register("magazine_loading", () -> new SimpleCraftingRecipeSerializer<>(MagazineLoadingRecipe::new));

    /** Gadget + su recarga → gadget cargado. */
    public static final RegistryObject<RecipeSerializer<com.bryanamaya.tecnocinetico.gear.GearRecipes.Recharge>> GADGET_RECHARGE =
            SERIALIZERS.register("gadget_recharge",
                    () -> new SimpleCraftingRecipeSerializer<>(com.bryanamaya.tecnocinetico.gear.GearRecipes.Recharge::new));
    /** Jetpack + kit de mejora → jetpack del nivel siguiente. */
    public static final RegistryObject<RecipeSerializer<com.bryanamaya.tecnocinetico.gear.GearRecipes.JetpackUpgrade>> JETPACK_UPGRADE =
            SERIALIZERS.register("jetpack_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(com.bryanamaya.tecnocinetico.gear.GearRecipes.JetpackUpgrade::new));

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
