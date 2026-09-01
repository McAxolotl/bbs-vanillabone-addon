package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * The bone list inside a pose keyframe's editor, as opened from a film or state timeline.
 *
 * <p>The host already has a mob form branch here, so bones were listed — but through the flat
 * overload, which fills a plain list and leaves the model and the flipped-part map null. Three
 * things ride on those: the tree structure, mirrored editing, and applying a transform to a bone's
 * children. All three were silently dead, while the addon's own form editor panel had them, so the
 * same pose editor behaved differently depending on where it was opened from.</p>
 *
 * <p>Redirecting to the model overload restores all three. Readable bone names come with it and
 * need nothing here: UIBoneTreeListMixin recognises this addon's IModel and relabels the rows it
 * builds, which is why the fork's extra labels() call has no counterpart.</p>
 */
@Mixin(value = UIPoseKeyframeFactory.class, remap = false)
public class UIPoseKeyframeFactoryMixin
{
    @Redirect(
        method = "<init>(Lmchorse/bbs_mod/utils/keyframes/Keyframe;Lmchorse/bbs_mod/ui/framework/elements/input/keyframes/UIKeyframes;)V",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/framework/elements/input/keyframes/factories/UIPoseKeyframeFactory$UIPoseFactoryEditor;fillGroups(Ljava/util/Collection;Z)V"
        )
    )
    private void bbsvanillabone$fillMobGroupsAsModel(
        UIPoseKeyframeFactory.UIPoseFactoryEditor editor,
        Collection<String> groups,
        boolean reset,
        @Local MobForm mobForm
    )
    {
        BoneHierarchy hierarchy = VanillaBoneTrackSheets.getBoneHierarchy(mobForm);

        if (hierarchy == null)
        {
            /* No hierarchy to describe — keep the host's flat list rather than an empty one. */
            editor.fillGroups(groups, reset);

            return;
        }

        editor.fillGroups(new VanillaModel(hierarchy), hierarchy.buildFlippedParts(), reset);
    }
}
