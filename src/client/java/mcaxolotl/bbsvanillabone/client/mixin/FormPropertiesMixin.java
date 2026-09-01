package mcaxolotl.bbsvanillabone.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.PerLimbService;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets per-bone pose channels ({@code pose.bones.<bone>}) reach mob forms during film and state
 * playback.
 *
 * <p>Channel creation is already type agnostic — {@code getOrCreate} routes any key containing
 * {@code pose.bones.} to the POSE_TRANSFORM factory — so the channels exist, deserialize and
 * interpolate for a mob form today. Only the application side is gated: both places that write a
 * bone channel back onto a form ask {@code instanceof ModelForm} and silently drop everything
 * else.</p>
 *
 * <p>That gate is a type check, not a capability difference. The whole branch uses one member,
 * {@code ModelForm.pose}, and the host's own MobForm has had a {@code public final ValuePose pose}
 * since before this addon existed — the same value this addon's renderer reads every frame.</p>
 *
 * <p>Both writes are covered here, and the pre-pass matters as much as the visible one: the
 * per-bone branch keeps a copy-on-write runtime pose, and the pre-pass is what drops it before the
 * frame's channels are added on top. Skipping it would leave every frame's rotation adding onto the
 * previous frame's result, which reads as bones drifting further and further from their keyframes
 * rather than as a missing feature.</p>
 */
@Mixin(value = FormProperties.class, remap = false)
public class FormPropertiesMixin
{
    /* Interpolation of the segment is the host's, not a reimplementation: blend < 1F has to go
     * through the channel's own keyframe factory (copy, then interpolate against itself) for a
     * fading animation state to release the way ModelForm's bones do. Shadowed rather than reached
     * through an @Invoker because the handler below is merged into FormProperties itself, so the
     * private method is directly callable and no new member has to be added to the host class. */
    @Shadow
    private Object interpolateValue(KeyframeChannel value, Object current, KeyframeSegment segment, float blend)
    {
        throw new AssertionError();
    }

    /**
     * The pre-pass that resets the copy-on-write runtime pose (FormProperties#applyProperties).
     *
     * <p>Attached to the form lookup rather than reimplemented at HEAD so that the host keeps
     * owning every condition around it: which channels count as bone channels, the once-per-form
     * dedupe, and the "skip the reset when a whole-pose channel drives this form anyway" guard.
     * The only thing added is the mob form case; the value is handed back untouched, so the host's
     * own ModelForm branch still runs right after this returns.</p>
     */
    @ModifyExpressionValue(
        method = "applyProperties(Lmchorse/bbs_mod/forms/forms/Form;FF)V",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/FormUtils;getForm(Lmchorse/bbs_mod/forms/forms/Form;Ljava/lang/String;)Lmchorse/bbs_mod/forms/forms/Form;"
        )
    )
    private Form bbsvanillabone$resetMobPoseRuntimeValue(Form targetForm)
    {
        if (targetForm instanceof MobForm mobForm)
        {
            mobForm.pose.setRuntimeValue(null);
        }

        return targetForm;
    }

    /**
     * The per-bone write itself (FormProperties#applyProperty).
     *
     * <p>Cancelling at HEAD is equivalent to widening the type check in place: the host's bone
     * branch returns unconditionally once the channel parses as a bone channel, whether or not the
     * target form matched, so nothing downstream of it can be reached for these channels. Anything
     * that is not a mob form bone channel is left uncancelled and handled by the host.</p>
     */
    @Inject(
        method = "applyProperty(FLmchorse/bbs_mod/forms/forms/Form;Lmchorse/bbs_mod/utils/keyframes/KeyframeChannel;F)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbsvanillabone$applyMobPoseBone(float tick, Form form, KeyframeChannel value, float blend, CallbackInfo info)
    {
        PerLimbService.PoseBonePath poseBonePath = PerLimbService.parsePoseBonePath(value.getId());

        if (poseBonePath == null)
        {
            return;
        }

        Form targetForm = FormUtils.getForm(form, poseBonePath.formPath());

        if (!(targetForm instanceof MobForm mobForm))
        {
            return;
        }

        ValuePose pose = mobForm.pose;
        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            /* Copy on write */
            if (pose.getRuntimeValue() == null)
            {
                pose.setRuntimeValue(pose.getOriginalValue().copy());
            }

            /* Pose#get inserts a fresh transform on a miss, so this is never null — the host's own
             * null branch here is unreachable and is not carried over. The bone key is the stable
             * id the pose is authored under (minecraft:zombie#main/head); short labels are a
             * display concern and never reach this map. */
            PoseTransform transform = pose.get().get(poseBonePath.bone());
            Transform interpolated = (Transform) this.interpolateValue(value, new PoseTransform(), segment, blend);

            transform.add(interpolated);
        }

        info.cancel();
    }
}
