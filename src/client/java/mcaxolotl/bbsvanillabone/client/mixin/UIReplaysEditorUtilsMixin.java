package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mcaxolotl.bbsvanillabone.client.forms.MobFormPose;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The two film-side helpers that a mob form's bone tracks need but that the host writes for model
 * forms only.
 *
 * <p>Neither is on the critical path for AC5 — the tracks record, save and replay without them —
 * but both are silently wrong otherwise: the per-form track filter cannot list bone tracks it never
 * sees, and the whole-pose gizmo drags from a base computed for the wrong key. The host has no
 * mixins of its own on this class.</p>
 */
@Mixin(value = UIReplaysEditorUtils.class, remap = false)
public class UIReplaysEditorUtilsMixin
{
    /**
     * Lists a mob form's bone tracks in the form's own track filter.
     *
     * <p>The host collects every track a form contributes by enumerating its properties and then
     * adding the model-only sub-tracks; the bone tracks are in the second group. Appending at RETURN
     * to the very list being returned is enough — the returned sheets are throwaway, used only for
     * their filter keys and colours, which is why a scratch FormProperties is fine here (the host's
     * own code in this method does the same).</p>
     */
    @Inject(method = "collectFormTrackSheets(Lmchorse/bbs_mod/forms/forms/Form;)Ljava/util/List;", at = @At("RETURN"))
    private static void bbsvanillabone$collectMobBoneTrackSheets(Form form, CallbackInfoReturnable<List<UIKeyframeSheet>> info)
    {
        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        VanillaBoneTrackSheets.addBoneTrackSheets(mobForm, new FormProperties(""), info.getReturnValue(), null);
    }

    /**
     * Fixes the additive rotation base of a mob form's whole-pose track in the film gizmo.
     *
     * <p>This is not a type gate but a key-format one. The host derives the bone key it looks up in
     * the pose from the gizmo's bone path with {@code StringUtils.fileName}, i.e. the last
     * slash-separated segment. That is right for a model bone, whose name has no slashes, and wrong
     * for a vanilla bone id: {@code minecraft:zombie#main/head} becomes {@code head}, which no pose
     * map holds, so the edited track's own contribution reads as zero and every rotation drag starts
     * from a base inflated by whatever the track already held.</p>
     *
     * <p>The bone is therefore taken straight off the pose editor's selection — the same value the
     * bone path was built from — instead of being recovered by string surgery. Per-bone tracks are
     * unaffected either way: they run through a different keyframe factory, for which the host
     * returns null here anyway, exactly as it does for model forms.</p>
     */
    @Inject(
        method = "filmPoseRotationBase(Lmchorse/bbs_mod/ui/framework/elements/input/keyframes/UIKeyframeEditor;Lmchorse/bbs_mod/forms/entities/IEntity;FLjava/lang/String;)Lorg/joml/Vector3f;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bbsvanillabone$mobFilmPoseRotationBase(
        UIKeyframeEditor keyframeEditor,
        IEntity entity,
        float transition,
        String bonePath,
        CallbackInfoReturnable<Vector3f> info
    )
    {
        if (!(keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory))
        {
            return;
        }

        UIKeyframeSheet sheet = keyframeEditor.getSheet(keyframeEditor.editor.getKeyframe());

        if (sheet == null)
        {
            return;
        }

        if (!(FormUtils.getProperty(entity.getForm(), sheet.id) instanceof ValuePose valuePose))
        {
            return;
        }

        if (!(FormUtils.getForm(valuePose) instanceof MobForm mobForm))
        {
            return;
        }

        String bone = poseFactory.poseEditor.groups.list.getCurrentFirst();

        if (bone == null || bone.isEmpty())
        {
            info.setReturnValue(null);

            return;
        }

        Vector3f evaluated = BaseFilmController.getGizmoBoneEvaluatedRotation(entity, transition, bonePath);

        info.setReturnValue(MobFormPose.additiveRotationBase(mobForm, valuePose, bone, evaluated));
    }
}
