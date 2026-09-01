package mcaxolotl.bbsvanillabone.client.forms;

import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

import java.util.List;

/**
 * The four form values this addon adds to the host's MobForm, reachable from addon code.
 *
 * <p>The values themselves are added by MobFormMixin, which implements this interface onto
 * MobForm. A duck interface is used rather than a static side table because these are real
 * ValueGroup children: they serialize with the form, survive a save/load round trip and show up in
 * the host's generic property enumeration. A side table would give none of that.</p>
 *
 * <p>Serialization keys match bbs-fsv exactly ({@code color}, {@code paused}, {@code bone_tracks},
 * {@code pose_overlay0..n}), so a form written by either build reads back on the other. The order
 * the values sit in differs — the host's own constructor already added mobId / mobNbt / pose /
 * pose_overlay / texture / slim by the time this addon can append, so texture and slim come before
 * these rather than between color and bone_tracks. That is display order in the property list and
 * key order in the serialized map, not a data difference.</p>
 */
public interface MobFormValues
{
    /** Tints the whole mob; blended with the form's additive mode by the renderer. */
    ValueColor bbsvanillabone$getColor();

    /** Freezes the animation clock while the skeleton stays editable. */
    ValueBoolean bbsvanillabone$getPaused();

    /** Whether the film timeline splits this form's pose into per-bone tracks. */
    ValueBoolean bbsvanillabone$getBoneTracks();

    /** The extra recording pose overlays, one per BBSSettings.recordingPoseTransformOverlays. */
    List<ValuePose> bbsvanillabone$getAdditionalOverlays();

    static MobFormValues of(MobForm form)
    {
        return (MobFormValues) form;
    }
}
