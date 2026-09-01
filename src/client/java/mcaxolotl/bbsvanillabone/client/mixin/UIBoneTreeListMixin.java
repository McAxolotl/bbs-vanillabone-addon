package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.ui.utils.bones.UIBoneTreeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Shows vanilla bones under readable short names in the bone tree, and puts them into the tree in
 * the first place on the one route where the host builds no rows for them at all.
 *
 * <p>A vanilla bone's id is a stable, mapping-independent key
 * ({@code minecraft:zombie#main/head}), which is unreadable as a list row. The addon already
 * computes short names (BoneHierarchy#getLabels): the entity's main layer keeps the plain field
 * name and every other layer is prefixed with its layer namespace, so a zombie's three
 * {@code head} parts read {@code head}, {@code inner_armor_head} and {@code outer_armor_head},
 * with a {@code _2} suffix as the last resort for a name that still collides. All this has to do
 * is get those labels into the rows.</p>
 *
 * <p><strong>The ids themselves never change.</strong> The tree list keeps holding ids as its
 * values, so everything downstream of a selection — pose keys, film bone tracks,
 * {@code BodyPart.bone}, an anchor's attachment path — keeps seeing the id. Only the label fields
 * of a row are swapped. A short name written into data would break saves, hence the display-only
 * rule.</p>
 *
 * <p>Why {@link ModifyArgs} on the constructor call and not the fsv shape: fsv added a public
 * {@code labels(Function)} setter and called it from each host, but three of those call sites are
 * host classes ({@code UIPoseKeyframeFactory}, {@code UIBodyPartEditor},
 * {@code UIBonePickerContextMenu}) an addon cannot edit. Resolving the label inside the list
 * instead covers every host at once, with no call-site change. The row type {@code Node} is a
 * private static nested class, so it can neither be constructed nor named from here — which rules
 * out {@code @Redirect} on the constructor; {@link ModifyArgs} only rewrites the arguments on the
 * stack and needs neither. {@link Local} picks up the walker's {@code model} parameter, and the
 * {@link VanillaModel} test is what keeps the host's own ModelForm trees untouched. The host has
 * no mixins of its own on this class.</p>
 *
 * <p>Scope: both walks that build rows. {@code boneNodes} is the bone <em>tree</em> behind
 * {@code setHierarchy} / {@code fillBones} — the form's bone editor panel, the pose keyframe
 * editor's list, and through UIBodyPartEditorMixin the attachment-bone picker as well.
 * {@code formNodes} / {@code formBoneNodes} is the <em>attachment</em> tree behind
 * {@code fillAttachments}, which is what every {@code UIAnchorKeyframeFactory.displayAttachments}
 * caller opens: the anchor keyframe editor, the tracker clip's attachment button and the two
 * axes-preview bone buttons. Surfaces that never build a row at all — film track titles, the
 * viewport hover cards, the attachment-bone button — are named at their own source instead.</p>
 */
@Mixin(value = UIBoneTreeList.class, remap = false)
public class UIBoneTreeListMixin
{
    /**
     * The tree walk builds {@code new Node(bone, bone, bone)}: id, tree row label, flat search
     * label. Arg 0 stays the id; args 1 and 2 become the short name so both the tree row and a
     * search result read the same.
     */
    @ModifyArgs(
        method = "boneNodes(Lmchorse/bbs_mod/cubic/IModel;Ljava/util/Collection;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/utils/bones/UIBoneTreeList$Node;<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
        )
    )
    private static void bbsvanillabone$labelVanillaBone(Args args, @Local(argsOnly = true) IModel model)
    {
        if (!(model instanceof VanillaModel vanillaModel))
        {
            return;
        }

        String label = vanillaModel.getLabel(args.get(0));

        if (label == null || label.isEmpty())
        {
            return;
        }

        args.set(1, label);
        args.set(2, label);
    }

    /**
     * Same for the attachment tree, whose rows are {@code new Node(key, bone, getTrackName(key))}:
     * the full attachment path, the tree row label, the flat search label. Arg 0 is the path the
     * anchor persists and stays untouched; arg 1 becomes the short name.
     *
     * <p>Arg 2 is a track name here, not a label, so it is only rewritten where the host itself fell
     * back to the raw path — {@code Form.getTrackName} returns its argument verbatim for a form with
     * no track name, which is exactly the case where a search result would read
     * {@code 2/minecraft:zombie#main/head}. The body part prefix is kept and only the bone id part
     * shortened ({@code 2/head}); a form that does carry a track name keeps the host's
     * {@code zombie/head}, since overwriting that would lose which actor the row belongs to.</p>
     */
    @ModifyArgs(
        method = "formBoneNodes(Lmchorse/bbs_mod/forms/forms/Form;Lmchorse/bbs_mod/cubic/IModel;Ljava/util/Collection;Ljava/lang/String;Ljava/util/Set;)Ljava/util/List;",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/utils/bones/UIBoneTreeList$Node;<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
        )
    )
    private static void bbsvanillabone$labelVanillaAttachment(Args args, @Local(argsOnly = true) IModel model)
    {
        if (!(model instanceof VanillaModel vanillaModel))
        {
            return;
        }

        String bone = args.get(1);
        String label = vanillaModel.getLabel(bone);

        if (label == null || label.isEmpty())
        {
            return;
        }

        String key = args.get(0);
        String trackName = args.get(2);

        args.set(1, label);

        if (key.equals(trackName) && key.endsWith(bone))
        {
            args.set(2, key.substring(0, key.length() - bone.length()) + label);
        }
    }

    /**
     * The attachment tree's missing mob form branch. {@code formNodes} builds bone rows only for a
     * {@code ModelForm}, so a mob form's bones used to reach the list through the safety net at the
     * end of {@code fillAttachments} instead: appended flat, in alphabetical order, outside their own
     * form's row and without metadata — which is also why the label handler above had nothing to
     * rewrite there. Running the host's own bone walk with this addon's {@link IModel} puts them
     * where a model form's bones are.
     *
     * <p>The keys match by construction: the walk keys a bone as
     * {@code StringUtils.combinePaths(formPath, bone)}, which is exactly what this addon's renderer
     * publishes into the matrix cache, and only keys present in the set become rows — so this can
     * only add rows the picker could already select, never invent one.</p>
     *
     * <p>Injected just before the body part walk, i.e. right after the ModelForm branch, so bones
     * come out ahead of the nested forms as a model form's do. {@code children} is the list both
     * branches feed and the host hangs off the form's own node afterwards. A mob form with no
     * resolvable hierarchy (no world, a host renderer this addon did not register, a model with no
     * bones) falls through to the safety net exactly as before.</p>
     */
    @Inject(
        method = "formNodes(Lmchorse/bbs_mod/forms/forms/Form;Ljava/lang/String;Ljava/util/Set;)Ljava/util/List;",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/forms/BodyPartManager;getAllTyped()Ljava/util/List;"
        )
    )
    private static void bbsvanillabone$nestVanillaBones(
        Form form,
        String path,
        Set<String> keys,
        CallbackInfoReturnable<List<Object>> info,
        @Local(ordinal = 0) List<Object> children
    )
    {
        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        BoneHierarchy hierarchy = VanillaBoneTrackSheets.getBoneHierarchy(mobForm);

        if (hierarchy == null)
        {
            return;
        }

        children.addAll(bbsvanillabone$formBoneNodes(form, new VanillaModel(hierarchy), hierarchy.getRootIds(), path, keys));
    }

    /**
     * The host's own recursive bone walk, reached rather than reimplemented: it is what builds the
     * {@code Node} rows, and {@code Node} is a private static nested class this addon can neither
     * name nor construct. The element type is {@code Object} for the same reason — the list is only
     * ever handed straight back to the host.
     */
    @Invoker("formBoneNodes")
    private static List<Object> bbsvanillabone$formBoneNodes(Form owner, IModel model, Collection<String> bones, String formPath, Set<String> keys)
    {
        throw new AssertionError("@Invoker was not applied");
    }
}
