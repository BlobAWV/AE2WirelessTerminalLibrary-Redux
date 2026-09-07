package tfar.ae2wt.recipe;

import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;

public class MagnetInstallSerializer extends SpecialRecipeSerializer<MagnetInstallRecipe> {
    public static final MagnetInstallSerializer INSTANCE = new MagnetInstallSerializer();
    public static final ResourceLocation ID = new ResourceLocation("ae2wtlib", "magnet_install");

    private MagnetInstallSerializer() {
        super(MagnetInstallRecipe::new);
    }
}