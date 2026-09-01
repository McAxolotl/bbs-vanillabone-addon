package mcaxolotl.bbsvanillabone.client.ui;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mcaxolotl.bbsvanillabone.client.forms.MobFormPose;
import mcaxolotl.bbsvanillabone.client.forms.MobFormValues;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Consumer;

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
     * Makes the general panel's "bone tracks" switch write to a mob form.
     *
     * <p>The host's own callback is written {@code if (this.form instanceof ModelForm m)}, so on a
     * mob form the toggle flips visually and stores nothing. Wrapping the callback rather than
     * mixing into the panel is possible because {@code UIClickable.callback} is a public field: the
     * host's own handler still runs first (it is a no-op here), then the value lands on the mob
     * form. No new UI element is introduced, so the switch keeps its place, tooltip and label.</p>
     */
    @Override
    protected void registerDefaultPanels()
    {
        super.registerDefaultPanels();

        Consumer<UIToggle> hostCallback = this.generalPanel.boneTracks.callback;

        this.generalPanel.boneTracks.callback = (toggle) ->
        {
            if (hostCallback != null)
            {
                hostCallback.accept(toggle);
            }

            if (this.form != null)
            {
                MobFormValues.of(this.form).bbsvanillabone$getBoneTracks().set(toggle.getValue());
            }
        };
    }

    /**
     * Shows that same switch, at the form's current value.
     *
     * <p>Has to run after {@code super}, since the host's panel hides the switch for anything that
     * is not a model form from inside its own startEdit.</p>
     */
    @Override
    public void startEdit(MobForm form, Class<?> preferredPanel)
    {
        super.startEdit(form, preferredPanel);

        this.generalPanel.boneTracks.setValue(MobFormValues.of(form).bbsvanillabone$getBoneTracks().get());
        this.generalPanel.boneTracks.setVisible(true);
        this.generalPanel.options.resize();
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

        return MobFormPose.additiveRotationBase(this.form, this.form.pose, bone, this.getEvaluatedRotation(transition, this.bonePath()));
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
