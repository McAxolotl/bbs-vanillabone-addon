package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import mcaxolotl.bbsvanillabone.client.bones.VanillaBoneLabels;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframePreviewTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The caption on a pose keyframe's hover preview.
 *
 * <p>The tooltip names the bone a per-bone keyframe belongs to, taken from the channel id, so for a
 * vanilla bone it read the full stable id in a tooltip sized for a short name.</p>
 */
@Mixin(value = UIKeyframePreviewTooltip.class, remap = false)
public class UIKeyframePreviewTooltipMixin
{
    @ModifyExpressionValue(
        method = "renderPose(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/utils/keyframes/Keyframe;)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/replays/PerLimbService$PoseBonePath;bone()Ljava/lang/String;")
    )
    private String bbsvanillabone$labelPreviewBone(String bone, @Local Form form)
    {
        return VanillaBoneLabels.of(form, bone);
    }
}
