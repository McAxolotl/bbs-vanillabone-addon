package mcaxolotl.bbsvanillabone.client.ui;

import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.utils.IWorldTransformProvider;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import org.joml.Matrix4f;

/**
 * World-transform source for the mob form's pose editor, feeding the transform context menu's
 * copy/paste-world-transform actions. There is no scene in a form preview, so "world" is the
 * preview's own space — stable, since its root does not move.
 *
 * <p>The host's own FormBoneWorldProvider only takes a UIModelForm, so this reimplements the same
 * three lines against the single-method public interface instead of touching the host.</p>
 *
 * <p>Re-samples on every call, which is what the world paste's finite differences require: the
 * matrices come from the renderer's collectMatrices, which re-applies the live form pose, so a nudge
 * to a channel shows up in the next sample.</p>
 */
public class VanillaBoneWorldProvider implements IWorldTransformProvider
{
    private final UIVanillaBoneMobForm form;
    private final UIPoseEditor poseEditor;

    public VanillaBoneWorldProvider(UIVanillaBoneMobForm form, UIPoseEditor poseEditor)
    {
        this.form = form;
        this.poseEditor = poseEditor;
    }

    @Override
    public boolean getWorldMatrix(Matrix4f out)
    {
        UIFormEditor editor = this.form.editor;
        String bone = this.poseEditor.getGroup();

        if (editor == null || bone == null || bone.isEmpty())
        {
            return false;
        }

        Matrix4f matrix = this.form.getOriginMatrix(editor.getSamplingTick());

        if (matrix == null)
        {
            return false;
        }

        out.set(matrix);

        return true;
    }
}
