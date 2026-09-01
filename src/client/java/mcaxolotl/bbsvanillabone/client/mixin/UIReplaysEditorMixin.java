package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mcaxolotl.bbsvanillabone.forms.MobFormValues;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts a mob form's per-bone tracks on the film timeline.
 *
 * <p>The timeline itself is entirely type agnostic — track category, channel creation, keyframe
 * insertion, persistence and playback all key off the {@code pose.bones.} prefix of a channel id,
 * never off a form type. What is gated is only the <em>building</em> of the track rows: the one
 * place that asks for bone sheets asks {@code instanceof ModelForm}, so a mob form's tracks are
 * simply never constructed. Everything downstream has been working all along and is why per-bone
 * channels already replay (see FormPropertiesMixin) even without any of this.</p>
 *
 * <p>All three injections are plain callbacks on host methods; nothing here rewrites host logic.
 * The host has no mixins of its own on this class.</p>
 */
@Mixin(value = UIReplaysEditor.class, remap = false)
public class UIReplaysEditorMixin
{
    @Shadow
    private UIReplaysEditor.ReplayCategory category;

    @Shadow
    private boolean actionsMode;

    @Shadow
    private boolean allMode;

    @Shadow
    private void setCategory(UIReplaysEditor.ReplayCategory c)
    {
        throw new AssertionError();
    }

    /**
     * The one injection AC5 stands on: appends this form's bone tracks once the host has finished
     * laying out the form's own property tracks.
     *
     * <p>TAIL, not HEAD, and no MixinExtras: the five things needed — the accumulated sheet list,
     * the pose-tab map, the depth map — are the host method's own parameters, and by TAIL the
     * form's block of sheets is the tail of {@code sheets}, so inserting there lands inside that
     * block. The host's ModelForm branch has already run and done nothing.</p>
     *
     * <p>The pose sheet is looked up in {@code sheets} rather than rebuilt, because
     * {@code poseTabs} and the dope sheet's fold state compare sheets by object identity: a
     * separately constructed sheet would leave the bone rows permanently unfolded, unindented, and
     * dropped from the timeline on the next category switch.</p>
     *
     * <p>Only the bone half of the host's branch is mirrored. Material texture sheets, the other
     * half, have no mob form counterpart.</p>
     */
    @Inject(
        method = "flushForm(Ljava/util/List;Ljava/util/List;Lmchorse/bbs_mod/forms/forms/Form;Ljava/util/Map;Ljava/util/Map;)V",
        at = @At("TAIL")
    )
    private void bbsvanillabone$addMobBoneTracks(
        List<UIKeyframeSheet> sheets,
        List<UIKeyframeSheet> formSheets,
        Form form,
        Map<UIKeyframeSheet, List<UIKeyframeSheet>> poseTabs,
        Map<UIKeyframeSheet, Integer> poseTabDepths,
        CallbackInfo info
    )
    {
        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        Replay replay = ((UIReplaysEditor) (Object) this).getReplay();

        if (replay == null)
        {
            return;
        }

        List<UIKeyframeSheet> boneSheets = new ArrayList<>();
        Map<String, Integer> depthBySheetId = new HashMap<>();

        VanillaBoneTrackSheets.addBoneTrackSheets(mobForm, replay.properties, boneSheets, depthBySheetId);

        if (boneSheets.isEmpty())
        {
            return;
        }

        for (UIKeyframeSheet boneSheet : boneSheets)
        {
            Integer depth = depthBySheetId.get(boneSheet.id);

            poseTabDepths.put(boneSheet, depth == null ? 0 : depth);
        }

        String path = FormUtils.getPath(form);
        String poseId = path.isEmpty() ? "pose" : path + FormUtils.PATH_SEPARATOR + "pose";
        int poseIndex = -1;

        for (int i = 0; i < sheets.size(); i++)
        {
            UIKeyframeSheet sheet = sheets.get(i);

            if (poseId.equals(sheet.id) && sheet.channel.getFactory() == KeyframeFactories.POSE)
            {
                poseIndex = i;
                break;
            }
        }

        if (poseIndex < 0)
        {
            sheets.addAll(boneSheets);

            return;
        }

        poseTabs.put(sheets.get(poseIndex), boneSheets);
        sheets.addAll(poseIndex + 1, boneSheets);
    }

    /**
     * Jumps to the Pose category when a mob form bone is picked in the viewport.
     *
     * <p>The pick itself is type agnostic; without this the click just finds no pose sheet in
     * whatever category is open and does nothing at all, silently. Not cancellable on purpose — the
     * host's own gate simply will not match a mob form afterwards, and its delegation to the shared
     * pick logic has to keep running.</p>
     */
    @Inject(method = "pickFormBone(Lmchorse/bbs_mod/forms/forms/Form;Ljava/lang/String;Z)V", at = @At("HEAD"))
    private void bbsvanillabone$pickMobFormBone(Form form, String bone, boolean insert, CallbackInfo info)
    {
        if (!(form instanceof MobForm) || bone == null || bone.isEmpty())
        {
            return;
        }

        if (this.allMode)
        {
            ((UIReplaysEditor) (Object) this).setActionsMode(false);
        }
        else if (this.category != UIReplaysEditor.ReplayCategory.POSE || this.actionsMode)
        {
            this.setCategory(UIReplaysEditor.ReplayCategory.POSE);
        }
    }

    /**
     * Adds the "poses to limbs" track action for mob forms.
     *
     * <p>The host's whole timeline context menu sits behind one
     * {@code replay.form.get() instanceof ModelForm}, and its body is compiled into a synthetic
     * lambda whose name carries a counter the compiler assigns — targeting that would break on any
     * unrelated host edit, and break as a startup crash in a user's hands rather than here. So
     * instead a second consumer is appended: {@code UIElement.context} adds to a list, it does not
     * replace, so this menu item simply follows the host's own. Cost is placement — it lands after
     * "Filter sheets" rather than beside the model form's copy of the same action.</p>
     */
    @Inject(method = "updateChannelsList()V", at = @At("TAIL"))
    private void bbsvanillabone$appendMobPoseTrackMenu(CallbackInfo info)
    {
        UIReplaysEditor editor = (UIReplaysEditor) (Object) this;

        /* No sheets at all means no keyframe editor was built. */
        if (editor.keyframeEditor == null)
        {
            return;
        }

        editor.keyframeEditor.view.context((menu) -> bbsvanillabone$offerPosesToLimbs(editor, menu));
    }

    private static void bbsvanillabone$offerPosesToLimbs(UIReplaysEditor editor, ContextMenuManager menu)
    {
        if (editor.keyframeEditor == null)
        {
            return;
        }

        UIKeyframeSheet sheet = editor.keyframeEditor.view.getGraph().getSheet(editor.getContext().mouseY);

        if (sheet == null || !sheet.selection.hasAny())
        {
            return;
        }

        /* The host's own test for "this is a whole-pose track, not an overlay and not a bone one". */
        boolean poseTrack = sheet.channel.getFactory() == KeyframeFactories.POSE
            && (sheet.id.equals("pose") || sheet.id.endsWith(FormUtils.PATH_SEPARATOR + "pose"))
            && !sheet.id.contains("pose_overlay");

        if (!poseTrack)
        {
            return;
        }

        Form sheetForm = sheet.property == null ? null : FormUtils.getForm(sheet.property);

        if (!(sheetForm instanceof MobForm mobForm) || !MobFormValues.of(mobForm).bbsvanillabone$getBoneTracks().get())
        {
            return;
        }

        menu.action(Icons.LIMB, UIKeys.FILM_REPLAY_CONTEXT_POSES_TO_LIMBS, () ->
        {
            VanillaBoneTrackSheets.posesToLimbTracks(editor.getReplay(), sheet, mobForm);

            sheet.selection.removeSelected();
            editor.updateChannelsList();
        });
    }
}
