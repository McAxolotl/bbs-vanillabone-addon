package mcaxolotl.bbsvanillabone.client.mixin;

import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The host keeps the mob form's stand-in entity in a private field with no getter, and rebuilds it
 * from the form's id and NBT in a private ensureEntity(). An addon renderer needs that same entity
 * to look up the vanilla EntityRenderer whose model layers the bones are discovered from, and
 * duplicating the entity's lifecycle here would mean two entities drifting apart.
 */
@Mixin(MobFormRenderer.class)
public interface MobFormRendererAccessor
{
    @Accessor("entity")
    Entity bbsvanillabone$getEntity();

    /**
     * Rebuilds the stand-in entity when the form's id or NBT changed. Reaching it directly matters:
     * the host also runs it from getBones(), but getBones() fills a static map of reflected field
     * names that the host's own LivingEntityRendererMixin treats as a licence to pose those parts.
     * Triggering entity setup through that side effect would hand the host a second way to apply
     * poses behind this addon's back.
     */
    @Invoker("ensureEntity")
    void bbsvanillabone$ensureEntity();
}
