package mcaxolotl.bbsvanillabone.client.bones;

import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;

/**
 * Readable bone names for the UI surfaces that are not the pose editor's own tree — viewport hover
 * cards, keyframe tooltips, film track titles, pickers.
 *
 * <p>The fork puts this on the host as {@code FormUtilsClient.getBoneLabel}, backed by a virtual
 * {@code getBoneHierarchy()} it adds to the renderer base class. An addon can add neither, so each
 * surface has to be reached on its own and they all come through here to keep one answer.</p>
 *
 * <p><strong>Display only.</strong> Pose keys, film bone track ids and {@code BodyPart.bone} all
 * keep the stable id; a short name written back into data would corrupt it quietly, because the bone
 * resolver accepts legacy aliases and would keep rendering correctly.</p>
 */
public final class VanillaBoneLabels
{
    private VanillaBoneLabels()
    {}

    /** The bone's short label, or the id itself for anything this addon does not name. */
    public static String of(Form form, String bone)
    {
        if (bone == null || bone.isEmpty() || !(form instanceof MobForm mobForm))
        {
            return bone;
        }

        BoneHierarchy hierarchy = VanillaBoneTrackSheets.getBoneHierarchy(mobForm);
        String label = hierarchy == null ? null : hierarchy.getLabel(bone);

        return label == null || label.isEmpty() ? bone : label;
    }
}
