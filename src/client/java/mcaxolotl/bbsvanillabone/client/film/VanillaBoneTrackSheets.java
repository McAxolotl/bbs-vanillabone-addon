package mcaxolotl.bbsvanillabone.client.film;

import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.forms.MobFormValues;
import mcaxolotl.bbsvanillabone.client.forms.VanillaBoneMobFormRenderer;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.PerLimbService;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The per-bone film timeline tracks of a mob form.
 *
 * <p>The host builds these in {@code UIReplaysEditorUtils.addBoneTrackSheets}, whose first
 * parameter is typed {@code ModelForm} — a signature an addon cannot widen, which is why the body
 * is reimplemented here instead of injected into. Nothing ModelForm-specific is lost in the move:
 * the host's version reads exactly two things, the form's {@code boneTracks} switch and a bone
 * hierarchy, and touches none of the model form's physics, IK, constraint, material or animation
 * state. Its one genuinely model-only step, skipping the bones a model marks disabled, has no mob
 * form counterpart (vanilla models carry no such marks), so it drops out rather than being
 * emulated.</p>
 *
 * <p>Everything else — the channel key rule, the hue-per-parent colouring, the LIMB icon, the
 * {@code isBoneTrack} flag, the detached {@code ValueTransform} seed — stays identical to the
 * host's, because the timeline reads all of it back through host code shared with model bone
 * tracks.</p>
 */
public final class VanillaBoneTrackSheets
{
    /** The host's own hue count for bone tracks, so a mob form's tracks colour like a model's. */
    private static final int BONE_TRACK_HUE_COUNT = 12;

    private VanillaBoneTrackSheets()
    {}

    /**
     * The form's bone hierarchy, or null when there is nothing to build tracks from.
     *
     * <p>The world check is not decoration: resolving the hierarchy goes through the host's
     * ensureEntity, which builds the stand-in entity with
     * {@code EntityType.create(MinecraftClient.getInstance().world)}, and vanilla's own MobEntity
     * constructor dereferences that world (it takes the profiler supplier off it). Every way into
     * the BBS dashboard needs an in-game context, so this should not trigger — but the callers here
     * run on every timeline rebuild, where an exception would take the whole film editor down
     * instead of just the bone tracks.</p>
     */
    public static BoneHierarchy getBoneHierarchy(MobForm form)
    {
        if (MinecraftClient.getInstance().world == null)
        {
            return null;
        }

        if (!(FormUtilsClient.getRenderer(form) instanceof VanillaBoneMobFormRenderer renderer))
        {
            return null;
        }

        BoneHierarchy hierarchy = renderer.getBoneHierarchy();

        return hierarchy.getBoneIds().isEmpty() ? null : hierarchy;
    }

    /**
     * Builds one sheet per bone, in hierarchy order, into {@code out}, and each one's indent depth
     * into {@code depthBySheetId} (keyed by sheet id, which is how the host hands depths on).
     */
    public static void addBoneTrackSheets(MobForm form, FormProperties properties, List<UIKeyframeSheet> out, Map<String, Integer> depthBySheetId)
    {
        if (!MobFormValues.of(form).bbsvanillabone$getBoneTracks().get())
        {
            return;
        }

        BoneHierarchy hierarchy = getBoneHierarchy(form);

        if (hierarchy == null)
        {
            return;
        }

        /* One adapter for the whole call: it builds its label table once on first use, whereas
         * BoneHierarchy#getLabels rebuilds the entire table per call. */
        VanillaModel model = new VanillaModel(hierarchy);
        List<String> bones = model.getGroupKeysInHierarchyOrder();
        Map<String, Integer> parentToColor = new HashMap<>();
        int[] hueIndex = {0};
        String path = FormUtils.getPath(form);

        for (String bone : bones)
        {
            String parent = model.getParentGroupKey(bone);
            int color = parentToColor.computeIfAbsent(parent, (p) ->
                Colors.HSVtoRGB((hueIndex[0]++ % BONE_TRACK_HUE_COUNT) / (float) BONE_TRACK_HUE_COUNT, 0.7F, 0.7F).getRGBColor()
            );

            /* The key carries the stable id in full (pose.bones.minecraft:zombie#main/head): it is
             * what the channel persists under and what playback resolves the bone by. The title
             * carries the short label the bone tree shows. The two must not be swapped — a short
             * name in the key yields a track that records and saves but moves nothing. */
            String boneKey = PerLimbService.toPoseBoneKey(path, bone);
            String title = path.isEmpty() ? labelOf(model, bone) : path + "/" + labelOf(model, bone);
            KeyframeChannel channel = properties.registerChannel(boneKey, KeyframeFactories.POSE_TRANSFORM);
            ValueTransform transform = new ValueTransform(boneKey, new PoseTransform());

            out.add(new UIKeyframeSheet(boneKey, IKey.constant(title), color, false, channel, transform, true).icon(Icons.LIMB).form(form));

            if (depthBySheetId != null)
            {
                depthBySheetId.put(boneKey, getBoneDepth(model, bone));
            }
        }
    }

    /**
     * Splits the selected whole-pose keyframes of {@code poseSheet} into per-bone keyframes — the
     * mob form half of the host's {@code posesToLimbTracks}. The caller drops the originals.
     */
    @SuppressWarnings("unchecked")
    public static void posesToLimbTracks(Replay replay, UIKeyframeSheet poseSheet, MobForm mobForm)
    {
        if (replay == null || poseSheet == null || mobForm == null)
        {
            return;
        }

        String formPath = poseSheet.id.equals("pose")
            ? ""
            : poseSheet.id.substring(0, poseSheet.id.length() - (FormUtils.PATH_SEPARATOR + "pose").length());
        Form form = formPath.isEmpty() ? replay.form.get() : FormUtils.getForm(replay.form.get(), formPath);

        /* Re-resolved from the replay rather than taken off the sheet, because the channels have to
         * be created against the form instance the replay plays back. Past its null check the host
         * ignores its own form parameter for the same reason. */
        if (!(form instanceof MobForm targetForm))
        {
            return;
        }

        BoneHierarchy hierarchy = getBoneHierarchy(targetForm);

        if (hierarchy == null)
        {
            return;
        }

        List<Keyframe<Pose>> selectedKeyframes = (List<Keyframe<Pose>>) (List<?>) poseSheet.selection.getSelected();

        if (selectedKeyframes.isEmpty())
        {
            return;
        }

        List<String> bones = hierarchy.getBoneIds();

        for (Keyframe<Pose> keyframe : selectedKeyframes)
        {
            Pose pose = keyframe.getValue();

            if (pose == null)
            {
                continue;
            }

            float tick = keyframe.getTick();

            for (String bone : bones)
            {
                String boneKey = PerLimbService.toPoseBoneKey(formPath, bone);
                KeyframeChannel<PoseTransform> limbChannel = (KeyframeChannel<PoseTransform>) replay.properties.getOrCreate(form, boneKey);

                if (limbChannel == null)
                {
                    continue;
                }

                /* Pose#get inserts a fresh transform on a miss, so an unposed bone contributes an
                 * identity keyframe rather than nothing — the host does the same, and that is what
                 * makes the split cover the whole skeleton. */
                PoseTransform copy = (PoseTransform) pose.get(bone).copy();
                int index = limbChannel.insert(tick, copy);

                limbChannel.get(index).copyOverExtra(keyframe);
            }
        }
    }

    private static String labelOf(VanillaModel model, String bone)
    {
        String label = model.getLabel(bone);

        return label == null || label.isEmpty() ? bone : label;
    }

    /**
     * Copied from the host's private getBoneDepth rather than read off
     * {@code BoneHierarchy.Bone#depth()}: the two agree today (a layer root is 0 either way, and
     * canonicalisation keeps a bone's parent inside its own layer), but only this one is defined by
     * the very parent chain the tracks are indented against, so it cannot drift out of step with
     * the parenting the bone tree draws.
     */
    private static int getBoneDepth(VanillaModel model, String bone)
    {
        int depth = 0;
        String current = bone;

        while (current != null && !current.isEmpty())
        {
            current = model.getParentGroupKey(current);

            if (current != null && !current.isEmpty())
            {
                depth++;
            }
        }

        return Math.max(0, depth);
    }
}
