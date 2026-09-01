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
 */
public final class MobFormPose
{
    private MobFormPose()
    {}

    /**
     * The host's additive rotation base, computed over every pose track a mob form has here.
     *
     * <p>Not delegated to {@code FormUtils.additivePoseRotationBase}. That helper builds its track
     * list from the two pose values upstream's MobForm declares, so it cannot see the
     * {@code pose_overlay0..n} this addon appends, and the omission costs more than the additivity
     * guard: the fallback sum would be short by the overlays' own rotation, and an edit to an
     * overlay track would find itself missing from the list and give up entirely. Recomputing the
     * whole thing here is the only way the answer matches bbs-fsv, where all the tracks sit in one
     * list to begin with.</p>
     *
     * <p>Reachable only when the recording overlay count is turned up — it defaults to zero, which
     * leaves the overlay list empty and every path below identical to the host's.</p>
     */
    public static Vector3f additiveRotationBase(MobForm form, ValuePose editedTrack, String bone, Vector3f evaluatedRadians)
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
        Vector3f editedContribution = new Vector3f();

        for (ValuePose track : tracks)
        {
            PoseTransform transform = track.get().transforms.get(bone);

            if (transform == null)
            {
                continue;
            }

            /* One multiplicative contributor and the additive-base model stops describing the
             * result at all, so there is no partial answer to give. */
            if (transform.rotationMode == Transform.RotationMode.QUATERNION || transform.fix != 0F)
            {
                return null;
            }

            if (track == editedTrack)
            {
                editedContribution.set(transform.rotate);
            }
            else
            {
                trackSum.add(transform.rotate);
            }
        }

        return evaluatedRadians == null ? trackSum : new Vector3f(evaluatedRadians).sub(editedContribution);
    }
}
