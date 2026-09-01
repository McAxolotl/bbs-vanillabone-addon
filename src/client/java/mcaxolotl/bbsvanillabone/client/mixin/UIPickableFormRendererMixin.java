package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mcaxolotl.bbsvanillabone.client.bones.VanillaBoneLabels;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;
import mchorse.bbs_mod.utils.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The hover card that names the form and bone under the cursor in a form editor viewport.
 *
 * <p>It read the bone id straight out of the picked pair, so hovering a vanilla bone produced a card
 * reading {@code minecraft:zombie#main/head} where a model form's would read {@code head}.</p>
 */
@Mixin(value = UIPickableFormRenderer.class, remap = false)
public class UIPickableFormRendererMixin
{
    /**
     * Only the second read of the pair's bone — the one being concatenated into the card. The first
     * is an emptiness test, and leaving it on the raw id keeps the decision about whether to show a
     * bone at all exactly where the host put it.
     */
    @ModifyExpressionValue(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "FIELD", target = "Lmchorse/bbs_mod/utils/Pair;b:Ljava/lang/Object;", ordinal = 1)
    )
    private Object bbsvanillabone$labelHoveredBone(Object bone, @Local Pair<Form, String> pair)
    {
        return VanillaBoneLabels.of(pair.a, (String) bone);
    }
}
