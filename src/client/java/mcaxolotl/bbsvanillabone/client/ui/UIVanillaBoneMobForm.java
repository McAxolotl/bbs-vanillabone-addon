package mcaxolotl.bbsvanillabone.client.ui;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mchorse.bbs_mod.ui.forms.editors.forms.UIMobForm;

/**
 * Stands in for the host's UIMobForm. Only the per-type panel stack is replaceable — the editor
 * shell (form tree, body part editing, gizmo, viewport picking) stays the host's, and it reaches
 * this class through a handful of overridable methods whose defaults all converge on
 * collectMatrices publishing matrix / origin / evaluatedRotation. Filling those in on the renderer
 * side is what makes the gizmo and picking work, so none of that is reimplemented here.
 *
 * Inherits everything for now; the bone editing panel arrives in the editor step.
 */
public class UIVanillaBoneMobForm extends UIMobForm
{
    private static boolean announced;

    public UIVanillaBoneMobForm()
    {
        if (!announced)
        {
            announced = true;

            BBSVanillaBoneClientAddon.LOGGER.info("mob form editor panel taken over: the host built a {}", this.getClass().getSimpleName());
        }
    }
}
