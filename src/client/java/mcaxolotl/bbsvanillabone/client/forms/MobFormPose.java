package mcaxolotl.bbsvanillabone.client.forms;

import mcaxolotl.bbsvanillabone.forms.MobFormValues;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The additive euler base a rotation drag composes on top of, for a mob form bone.
 *
 * <p>Shared by the two places that ask for it — the form editor's gizmo and the film timeline's
 * gizmo — so the same drag cannot behave differently depending on which one is open.</p>
 *
 * <p><strong>Only sibling pose tracks belong in this number.</strong> The gizmo's contract is that
 * the renderer shows {@code ZYX(base + channels)}, and for a vanilla bone only half of what reaches
 * the bone is additive: overlay tracks really are summed per channel, but the animation angle
 * vanilla writes is composed as a separate quaternion factor. That factor is already accounted for —
 * the drag measures its rotate axes numerically, by perturbing each channel and resampling the
 * matrix, so a multiplicative outer factor is absorbed exactly. Feeding it in here as well
 * double-counts it: the recovered parent frame stops being a rotation, the drag gain drifts, and the
 * euler branch flip that keeps {@code ZYX(total)} continuous stops keeping {@code ZYX(total - base)}
 * continuous, which is a hard jump mid-drag.</p>
 */
public final class MobFormPose
{
    private MobFormPose()
    {}

    /**
     * The summed rotation of every pose track on this form except the one being edited.
     *
     * <p>Zero unless recording overlays are turned on, which is what makes the common case behave
     * exactly like the fork — the fork returns nothing here at all. Where the two differ is with
     * overlays present: those are genuinely additive, and leaving them out of the base makes a drag
     * miss by their rotation.</p>
     */
    public static Vector3f additiveRotationBase(MobForm form, ValuePose editedTrack, String bone)
    {
        List<ValuePose> tracks = new ArrayList<>();

        tracks.add(form.pose);
        tracks.add(form.poseOverlay);
        tracks.addAll(MobFormValues.of(form).bbsvanillabone$getAdditionalOverlays());

        if (!tracks.contains(editedTrack))
        {
            return null;
        }

        Vector3f trackSum = new Vector3f();

        for (ValuePose track : tracks)
        {
            PoseTransform transform = track.get().transforms.get(bone);

            if (transform == null || track == editedTrack)
            {
                continue;
            }

            /* One multiplicative contributor and the additive model stops describing the stack at
             * all, so there is no partial answer to give. */
            if (transform.rotationMode == Transform.RotationMode.QUATERNION || transform.fix != 0F)
            {
                return null;
            }

            trackSum.add(transform.rotate);
        }

        return trackSum;
    }
}
