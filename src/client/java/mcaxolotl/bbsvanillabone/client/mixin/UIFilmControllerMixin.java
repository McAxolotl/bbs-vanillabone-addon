package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mcaxolotl.bbsvanillabone.client.bones.VanillaBoneLabels;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.utils.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The hover card that names the form and bone under the cursor in the film monitor.
 *
 * <p>The film viewport's twin of {@link UIPickableFormRendererMixin}: same card, same
 * {@code form - bone} text built off the same picked pair, drawn by a different owner. It read the
 * bone id straight out of the pair, so hovering a vanilla bone in a film produced a card reading
 * {@code minecraft:boat#main/right_paddle} where a model form's would read {@code right_paddle}.</p>
 *
 * <p>The host writes the card twice — once for Alt held (where hovering a <em>different</em> replay
 * shows that replay's name instead) and once for the plain case — so both copies are patched. The
 * host has no mixins of its own on this class.</p>
 */
@Mixin(value = UIFilmController.class, remap = false)
public class UIFilmControllerMixin
{
    /**
     * Alt held, cursor on the selected replay: the second of that branch's two reads of the pair's
     * bone, which is the one concatenated into the card.
     *
     * <p>The first read is an emptiness test and is deliberately left on the raw id, as in
     * {@link UIPickableFormRendererMixin}: the decision about whether to name a bone at all stays
     * where the host put it, and the label lookup rebuilds its whole table per call, so the card
     * costs one lookup per frame rather than two.</p>
     */
    @ModifyExpressionValue(
        method = "renderPickingPreview(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/utils/Area;)V",
        at = @At(value = "FIELD", target = "Lmchorse/bbs_mod/utils/Pair;b:Ljava/lang/Object;", ordinal = 1)
    )
    private Object bbsvanillabone$labelHoveredBoneWithAlt(Object bone, @Local Pair<Form, String> pair)
    {
        return VanillaBoneLabels.of(pair.a, (String) bone);
    }

    /** The plain case, same shape: the fourth read of the pair's bone is the second concatenation. */
    @ModifyExpressionValue(
        method = "renderPickingPreview(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/utils/Area;)V",
        at = @At(value = "FIELD", target = "Lmchorse/bbs_mod/utils/Pair;b:Ljava/lang/Object;", ordinal = 3)
    )
    private Object bbsvanillabone$labelHoveredBone(Object bone, @Local Pair<Form, String> pair)
    {
        return VanillaBoneLabels.of(pair.a, (String) bone);
    }
}
