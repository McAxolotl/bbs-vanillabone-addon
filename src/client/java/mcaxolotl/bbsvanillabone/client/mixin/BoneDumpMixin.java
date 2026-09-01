package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mcaxolotl.bbsvanillabone.client.bones.VanillaBoneHierarchy;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TEMPORARY. Bone discovery has no user-visible surface until the editor panel lands, so this is
 * the only way to observe that layers resolve to the intended stable ids. Delete once the bone
 * tree is on screen.
 *
 * Dumps one layer's hierarchy, chosen by the -Dbbsvanillabone.dumpLayer prefix (default
 * "minecraft:zombie"); an empty value dumps every layer, which is loud.
 *
 * Registers the tree itself rather than reading back what EntityModelLoaderMixin registered:
 * both inject at the same RETURN and their relative order is undefined, so a read would come up
 * empty half the time. register is synchronized and rebuilds from the same root, so doing it
 * twice for the filtered layer costs a redundant walk and nothing else.
 */
@Mixin(EntityModelLoader.class)
public class BoneDumpMixin
{
    private static final String FILTER = System.getProperty("bbsvanillabone.dumpLayer", "minecraft:zombie");

    @Inject(method = "getModelPart", at = @At("RETURN"))
    private void bbsvanillabone$dumpBones(EntityModelLayer layer, CallbackInfoReturnable<ModelPart> info)
    {
        String layerId = VanillaBoneHierarchy.toLayerId(layer);

        if (!layerId.startsWith(FILTER))
        {
            return;
        }

        ModelPart root = info.getReturnValue();

        if (root == null)
        {
            BBSVanillaBoneClientAddon.LOGGER.warn("bone dump: {} baked a null root", layerId);

            return;
        }

        VanillaBoneHierarchy.Hierarchy hierarchy = VanillaBoneHierarchy.register(layer, root);

        BBSVanillaBoneClientAddon.LOGGER.info("bone dump: {} ({} bones)", hierarchy.getLayerId(), hierarchy.getBones().size());

        for (VanillaBoneHierarchy.Bone bone : hierarchy.getBones())
        {
            BBSVanillaBoneClientAddon.LOGGER.info("  {}{}  ->  {}", "  ".repeat(bone.getDepth()), bone.getName(), bone.getId());
        }
    }
}
