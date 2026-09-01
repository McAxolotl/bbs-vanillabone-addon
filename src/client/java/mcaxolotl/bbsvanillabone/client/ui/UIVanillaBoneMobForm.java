package mcaxolotl.bbsvanillabone.client.ui;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mcaxolotl.bbsvanillabone.client.forms.MobFormValues;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Stands in for the host's UIMobForm. Only the per-type panel stack is replaceable — the editor
 * shell (form tree, body part editing, gizmo, viewport picking) stays the host's and reaches this
 * class through a handful of overridable methods.
 *
 * <p>Extends UIForm directly rather than the host's UIMobForm: that constructor installs a
 * UIMobFormPanel as the default panel, which would have to be torn back out. Nothing in the host
 * refers to UIMobForm as a type — it is only mentioned in the registration this addon overwrites —
 * so dropping it costs nothing.</p>
 *
 * <p>The overrides here all serve one purpose: pointing the shell at the selected BONE instead of
 * the form root. The matrices themselves are published by the renderer's collectMatrices, so no
 * gizmo or picking code lives on this side.</p>
 */
public class UIVanillaBoneMobForm extends UIForm<MobForm>
{
    private static boolean announced;

    public UIVanillaBoneMobFormPanel mobPanel;

    public UIVanillaBoneMobForm()
    {
        super();

        this.mobPanel = new UIVanillaBoneMobFormPanel(this);
        this.mobPanel.poseEditor.transform.hotkeyDrag(() -> this.editor == null ? null : this.editor.buildHotkeyDrag(this.mobPanel.poseEditor.transform));
        this.mobPanel.poseEditor.transform.worldTransform(new VanillaBoneWorldProvider(this, this.mobPanel.poseEditor));
        this.defaultPanel = this.mobPanel;

        this.registerPanel(this.defaultPanel, UIKeys.FORMS_EDITORS_MOB_TITLE, Icons.MORPH);
        this.registerDefaultPanels();

        if (!announced)
        {
            announced = true;

            BBSVanillaBoneClientAddon.LOGGER.info("mob form editor panel taken over: the host built a {}", this.getClass().getSimpleName());
        }
    }

    /**
     * What the viewport gizmo writes into. Also deliberately does NOT switch to the general panel
     * the way the base implementation does — the gizmo edits the bone the pose editor has selected.
     */
    @Override
    public UIPropTransform getEditableTransform()
    {
        return this.mobPanel.poseEditor.transform;
    }

    /** Where the gizmo is drawn, and where its translate Jacobian is sampled. */
    @Override
    public Matrix4f getOrigin(float transition)
    {
        return this.getOrigin(transition, this.bonePath(), this.mobPanel.poseEditor.transform.isLocal());
    }

    /**
     * The rotation-bearing sample for the gizmo's rotate axes. {@code local} stays true regardless
     * of the UI's LOCAL/GLOBAL toggle: in a rotation-stripped origin matrix the perturbation leaves
     * no trace and axis extraction silently collapses to identity.
     */
    @Override
    public Matrix4f getOriginMatrix(float transition)
    {
        return this.getOrigin(transition, this.bonePath(), true);
    }

    @Override
    public TransformSpace getGizmoSpace()
    {
        return this.mobPanel.poseEditor.transform.getSpace();
    }

    /*
     * getBodyPartGizmoOrigin is intentionally NOT overridden: body part mode edits the attachment
     * transform, for which the form's own root frame is the right origin and the bone selection is
     * noise. The base class documents that.
     */

    /**
     * The path the renderer publishes this bone's matrices under. The renderer keys bones with the
     * same combinePaths(prefix, boneId), so both sides line up without translation.
     */
    private String bonePath()
    {
        return StringUtils.combinePaths(FormUtils.getPath(this.form), this.mobPanel.poseEditor.groups.list.getCurrentFirst());
    }

    /**
     * The additive euler base under the pose editor's channels for the selected bone: the bone's
     * evaluated rotation (vanilla's own animation plus the whole pose stack) minus the edited track's
     * own contribution, so gizmo deltas compose at the angle the bone actually renders at.
     *
     * <p>This matters more for vanilla bones than for model bones: vanilla setAngles writes non-zero
     * angles to heads and limbs every frame, so a zero base would put every rotation drag out by
     * whatever the animation was doing.</p>
     *
     * <p>Public because the shell's own gate on it is private and typed to the model form; the
     * UIFormEditorMixin forwards here.</p>
     */
    public Vector3f poseRotationBase(UIPropTransform transform, float transition)
    {
        if (transform != this.mobPanel.poseEditor.transform)
        {
            return null;
        }

        String bone = this.mobPanel.poseEditor.groups.list.getCurrentFirst();

        if (bone == null)
        {
            return null;
        }

        /* FormUtils.additivePoseRotationBase weighs additivity over the two pose tracks the host's
         * MobForm has, so it cannot see the recording overlays this addon adds. A multiplicative
         * contributor in any track means the additive-base model does not apply at all, so those
         * overlays are checked here — otherwise the answer would differ from bbs-fsv, which has all
         * the tracks in one list. */
        for (ValuePose overlay : MobFormValues.of(this.form).bbsvanillabone$getAdditionalOverlays())
        {
            PoseTransform poseTransform = overlay.get().transforms.get(bone);

            if (poseTransform != null && (poseTransform.rotationMode == Transform.RotationMode.QUATERNION || poseTransform.fix != 0F))
            {
                return null;
            }
        }

        return FormUtils.additivePoseRotationBase(this.form.pose, bone, this.getEvaluatedRotation(transition, this.bonePath()));
    }

    /**
     * Ctrl+click in the viewport. Returning true is what stops the shell from carrying on into a
     * full editor rebuild, which is how the selection accumulates instead of resetting.
     */
    @Override
    public boolean toggleBoneSelection(String bone)
    {
        if (!this.mobPanel.poseEditor.hasBone(bone))
        {
            return false;
        }

        this.mobPanel.poseEditor.selectBone(bone, true);

        return true;
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.put("bones", DataStorageUtils.stringListToData(this.mobPanel.poseEditor.groups.list.getCurrent()));
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        if (data.has("bones"))
        {
            this.mobPanel.poseEditor.restoreSelection(DataStorageUtils.stringListFromData(data.get("bones")));
        }
    }
}
