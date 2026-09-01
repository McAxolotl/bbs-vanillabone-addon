package mcaxolotl.bbsvanillabone.mixin;

import mcaxolotl.bbsvanillabone.forms.MobFormValues;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the four form values the vanilla bone feature needs but the host's MobForm does not have:
 * a whole-mob colour, an animation pause switch, the per-bone film track switch, and the extra
 * recording pose overlays.
 *
 * <p>A MobForm subclass is not an option: the host resolves renderers and editor panels with
 * map.get(form.getClass()), and its MapFactory maps the class literally, so anything but MobForm
 * itself is invisible to both. The values are therefore merged into MobForm and handed out through
 * MobFormValues.</p>
 *
 * <p>They are created and registered from a constructor RETURN inject rather than from field
 * initialisers, so there is no question about how Mixin transplants initialisers. ValueGroup.add is
 * public and the merged code runs as part of MobForm's own constructor, which is what makes these
 * behave like the host's own values: serialized, enumerated as properties, keyframable.</p>
 *
 * <p><strong>This is the one part of the addon that has to apply on a dedicated server</strong>,
 * which is why it lives in the common source set and its config (bbsvanillabone.mixins.json) is
 * declared without an environment. The host builds real MobForm instances server side: a film LOAD
 * reads the film on the server and sends film.toData() back to the client
 * (ServerNetwork.LOAD / SAVE), and morph and form sync round trip forms the same way. ValueGroup
 * .fromData assigns only the keys the group already holds and drops the rest, so a client-only
 * version of this mixin would have a server strip color / paused / bone_tracks /
 * pose_overlay0..n out of every form that passes through it, and the client's next save would
 * write the stripped form back — silent, permanent data loss. The price is that the server needs
 * the addon installed as well.</p>
 *
 * <p>BBSSettings.recordingPoseTransformOverlays is read unguarded, exactly as the host's own
 * ModelForm constructor reads it — a MobForm built before the settings exist would already have
 * broken ModelForm.</p>
 */
@Mixin(MobForm.class)
public class MobFormMixin implements MobFormValues
{
    @Unique
    private ValueColor bbsvanillabone$color;

    @Unique
    private ValueBoolean bbsvanillabone$paused;

    @Unique
    private ValueBoolean bbsvanillabone$boneTracks;

    @Unique
    private List<ValuePose> bbsvanillabone$additionalOverlays;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsvanillabone$addValues(CallbackInfo info)
    {
        MobForm form = (MobForm) (Object) this;

        this.bbsvanillabone$color = new ValueColor("color", Color.white());
        this.bbsvanillabone$paused = new ValueBoolean("paused", false);
        this.bbsvanillabone$boneTracks = new ValueBoolean("bone_tracks", true);
        this.bbsvanillabone$additionalOverlays = new ArrayList<>();

        for (int i = 0; i < BBSSettings.recordingPoseTransformOverlays.get(); i++)
        {
            ValuePose valuePose = new ValuePose("pose_overlay" + i, new Pose());

            this.bbsvanillabone$additionalOverlays.add(valuePose);
            form.add(valuePose);
        }

        form.add(this.bbsvanillabone$color);
        this.bbsvanillabone$boneTracks.invisible();
        form.add(this.bbsvanillabone$boneTracks);
        form.add(this.bbsvanillabone$paused);
    }

    @Override
    public ValueColor bbsvanillabone$getColor()
    {
        return this.bbsvanillabone$color;
    }

    @Override
    public ValueBoolean bbsvanillabone$getPaused()
    {
        return this.bbsvanillabone$paused;
    }

    @Override
    public ValueBoolean bbsvanillabone$getBoneTracks()
    {
        return this.bbsvanillabone$boneTracks;
    }

    @Override
    public List<ValuePose> bbsvanillabone$getAdditionalOverlays()
    {
        return this.bbsvanillabone$additionalOverlays;
    }
}
