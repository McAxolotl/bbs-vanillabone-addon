package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mcaxolotl.bbsvanillabone.client.forms.MobFormPose;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * The four helpers on this class that a mob form needs but that the host writes for model forms
 * only. The host has no mixins of its own here.
 *
 * <p>Two concern the form's own bone tracks and are cosmetic-to-wrong rather than blocking: the
 * per-form track filter cannot list bone tracks it never sees, and the whole-pose gizmo drags from a
 * base computed for the wrong key. The tracks record, save and replay without either.</p>
 *
 * <p>The other two are the viewport bone menus, and those are not cosmetic — see
 * {@link #bbsvanillabone$offerMobBones}, where the host's silent return costs the click itself, not
 * just the menu.</p>
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

    /** Ctrl+click's sibling-bone menu, for a mob form. */
    @Inject(
        method = "offerAdjacent(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/forms/forms/Form;Ljava/lang/String;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bbsvanillabone$offerMobAdjacent(UIContext context, Form form, String bone, Consumer<String> consumer, CallbackInfo info)
    {
        if (bbsvanillabone$offerMobBones(context, form, bone, consumer, false))
        {
            info.cancel();
        }
    }

    /** Shift+click's ancestor-chain menu, for a mob form. */
    @Inject(
        method = "offerHierarchy(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/forms/forms/Form;Ljava/lang/String;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bbsvanillabone$offerMobHierarchy(UIContext context, Form form, String bone, Consumer<String> consumer, CallbackInfo info)
    {
        if (bbsvanillabone$offerMobBones(context, form, bone, consumer, true))
        {
            info.cancel();
        }
    }

    /**
     * The bone menu of a mob form: either the bones sharing the clicked bone's parent, or its chain
     * from itself up to the layer root, each entry handing that bone to the caller's own consumer.
     * Returns whether the call was handled.
     *
     * <p><strong>Why this is not just a missing menu.</strong> Every caller treats the host's method
     * as having handled the gesture. {@code UIFormEditor.pickFormFromRenderer} is
     * {@code if (Shift) offerHierarchy(...); else pickFormBone(...)} and
     * {@code UIAnimationStateEditor.pickFormFromRenderer} has that shape for Ctrl and Shift both, so
     * a host method that returns without doing anything takes the plain pick down with it;
     * {@code pickFormWithOffers} returns true either way, which the film viewport reads as consumed.
     * The host's body is a single {@code instanceof ModelForm} branch, so on a mob form Shift+click
     * neither opened a menu nor selected the bone — it did nothing at all, with no way for the user
     * to tell why.</p>
     *
     * <p>The host's disabled-bone filter drops out rather than being emulated: it reads marks a bbs
     * model carries and a vanilla model has none. Entries are labelled with the same short names the
     * bone tree shows, while the value handed to the consumer stays the stable id.</p>
     *
     * <p>Unlike bbs-fsv, an empty bone list falls through instead of replacing the context menu with
     * an empty one — {@code ContextMenuManager.create} hands back a menu with no actions rather than
     * null, and an empty box is a worse answer than the host's own no-op. It takes a bone id the
     * hierarchy does not know to get there, which the viewport pick that produced it should not
     * yield.</p>
     */
    private static boolean bbsvanillabone$offerMobBones(UIContext context, Form form, String bone, Consumer<String> consumer, boolean hierarchy)
    {
        if (!(form instanceof MobForm mobForm) || bone == null || bone.isEmpty())
        {
            return false;
        }

        /* Guarded entry: null world, a host renderer this addon did not register, or a model with no
         * bones at all. Going straight to the renderer would trip over the first of those. */
        BoneHierarchy boneHierarchy = VanillaBoneTrackSheets.getBoneHierarchy(mobForm);

        if (boneHierarchy == null)
        {
            return false;
        }

        VanillaModel model = new VanillaModel(boneHierarchy);
        Collection<String> bones = hierarchy ? model.getHierarchyGroups(bone) : model.getAdjacentGroups(bone);

        if (bones.isEmpty())
        {
            return false;
        }

        context.replaceContextMenu((menu) ->
        {
            for (String boneId : bones)
            {
                menu.action(Icons.LIMB, IKey.constant(model.labelOrId(boneId)), () -> consumer.accept(boneId));
            }

            menu.autoKeys();
        });

        return true;
    }
}
