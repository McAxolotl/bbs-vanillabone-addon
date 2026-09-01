package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.ui.forms.editors.states.keyframes.UIAnimationStateEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Bone tracks in the animation state editor.
 *
 * <p>Playback already reached mob forms — a state's per-bone channels are applied through the same
 * FormProperties path the film uses — so the asymmetry this fixes was a state that plays back bone
 * animation the editor cannot show or author. Of the host's three calls into the bone sheet
 * builder, the film editor's two are covered by UIReplaysEditorMixin; this is the third.</p>
 */
@Mixin(value = UIAnimationStateEditor.class, remap = false)
public class UIAnimationStateEditorMixin
{
    /**
     * Appends the mob form's bone sheets the way the host appends a model form's.
     *
     * <p>Flat rather than folded into pose tabs, because that is what the host does here too — the
     * state editor's flushForm takes no tab maps, unlike the film editor's.</p>
     */
    @Inject(
        method = "flushForm(Ljava/util/List;Ljava/util/List;Lmchorse/bbs_mod/forms/forms/Form;)V",
        at = @At("TAIL")
    )
    private void bbsvanillabone$addMobBoneTracks(List<UIKeyframeSheet> sheets, List<UIKeyframeSheet> formSheets, Form form, CallbackInfo info)
    {
        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        /* getState() is public, so the state's properties need no accessor. It is null while the
         * editor has no state selected, and the host's own flushForm would have thrown first in
         * that case — but this runs after it, so the check has to be here. */
        UIAnimationStateEditor editor = (UIAnimationStateEditor) (Object) this;

        if (editor.getState() == null)
        {
            return;
        }

        VanillaBoneTrackSheets.addBoneTrackSheets(mobForm, editor.getState().properties, sheets, null);
    }
}
