package mcaxolotl.bbsvanillabone.client.forms;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;

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
     * {@link FormUtils#additivePoseRotationBase} plus the recording overlays this addon adds.
     *
     * <p>The host's helper weighs additivity across the two pose tracks its own MobForm has, so it
     * cannot see the {@code pose_overlay0..n} values this addon appends. A multiplicative
     * contributor in any track means the additive-base model does not apply at all, hence the extra
     * pass — without it the answer would differ from bbs-fsv, where every track sits in one list.</p>
     */
    public static Vector3f additiveRotationBase(MobForm form, ValuePose editedTrack, String bone, Vector3f evaluatedRadians)
    {
        for (ValuePose overlay : MobFormValues.of(form).bbsvanillabone$getAdditionalOverlays())
        {
            PoseTransform poseTransform = overlay.get().transforms.get(bone);

            if (poseTransform != null && (poseTransform.rotationMode == Transform.RotationMode.QUATERNION || poseTransform.fix != 0F))
            {
                return null;
            }
        }

        return FormUtils.additivePoseRotationBase(editedTrack, bone, evaluatedRadians);
    }
}
