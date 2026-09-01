package mcaxolotl.bbsvanillabone.client.ui;

import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.forms.MobFormValues;
import mcaxolotl.bbsvanillabone.client.forms.VanillaBoneMobFormRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stands in for the host's UIMobFormPanel, adding the bone editor the host has no equivalent of.
 *
 * <p>None of the pose widgets are written here: the host already ships the whole stack
 * (UIModelPoseEditor with its bone tree, per-bone transform, fix, colour and lighting) and drives
 * it from an {@link mchorse.bbs_mod.cubic.IModel}, so a vanilla bone hierarchy reaches it through
 * the VanillaModel adapter and nothing else is needed. The tree rows read as short names
 * (mcaxolotl.bbsvanillabone.client.mixin.UIBoneTreeListMixin) while the list values stay the
 * stable ids (minecraft:zombie#main/head) that the pose is keyed by.</p>
 */
public class UIVanillaBoneMobFormPanel extends UIFormPanel<MobForm>
{
    private static final List<String> MOB_IDS = new ArrayList<>();

    public UIButton pickMob;
    public UIButton pick;
    public UIColor color;
    public UIToggle paused;
    public UIToggle slim;
    public UISection nbtSection;
    public UITextarea<TextLine> mobNBT;
    public UIModelPoseEditor poseEditor;

    static
    {
        for (RegistryKey<EntityType<?>> key : Registries.ENTITY_TYPE.getKeys())
        {
            MOB_IDS.add(key.getValue().toString());
        }

        /* The player entity type is never registered in the registry, so the host's list cannot
         * select it — and the slim toggle only means anything for a player mob. Listed explicitly
         * so that toggle stays reachable now that it only shows for players. */
        if (!MOB_IDS.contains("minecraft:player"))
        {
            MOB_IDS.add("minecraft:player");
        }

        MOB_IDS.sort(Comparator.naturalOrder());
    }

    public UIVanillaBoneMobFormPanel(UIForm editor)
    {
        super(editor);

        this.pickMob = new UIButton(VanillaBoneKeys.FORMS_EDITORS_MOB_PICK_MOB, (b) ->
        {
            UIListOverlayPanel list = new UIListOverlayPanel(VanillaBoneKeys.FORMS_EDITORS_MOB_MOBS, (id) ->
            {
                this.form.mobID.set(id);

                /* Rebuilds the panel: the bone tree belongs to the previous entity's model layers,
                 * and so does the stand-in entity the renderer discovers them from. */
                this.editor.startEdit(this.form);
            });

            list.addValues(MOB_IDS);
            list.setValue(this.form.mobID.get());

            UIOverlay.addOverlay(this.getContext(), list);
        });

        this.pick = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            Link link = this.form.texture.get();

            UITexturePicker.open(this.getContext(), link, (l) -> this.form.texture.set(l));
        });
        this.color = new UIColor((c) -> MobFormValues.of(this.form).bbsvanillabone$getColor().set(Color.rgba(c))).withAlpha();
        this.paused = new UIToggle(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_PAUSED, (b) -> MobFormValues.of(this.form).bbsvanillabone$getPaused().set(b.getValue()));
        this.slim = new UIToggle(UIKeys.FORMS_EDITOR_SLIM, (b) -> this.form.slim.set(b.getValue()));
        this.slim.tooltip(UIKeys.FORMS_EDITOR_SLIM_TOOLTIP);

        this.mobNBT = new UITextarea<>((t) -> this.form.mobNBT.set(t));
        this.mobNBT.background().h(160);
        this.mobNBT.wrap();
        this.nbtSection = this.section(UIKeys.SELECTORS_NBT, "mob_nbt", false);
        this.nbtSection.fields.add(this.mobNBT);

        this.poseEditor = new UIModelPoseEditor();
        this.poseEditor.transform.barBackground();

        this.options.add(this.pickMob, this.pick, this.color, this.paused);
    }

    /**
     * The form's vanilla bone hierarchy, or an empty one when the renderer is not this addon's.
     * The addon registers its own renderer under MobForm.class, so the cast normally holds; the
     * fallback keeps a stale or foreign registration from throwing inside the editor.
     */
    private BoneHierarchy getBoneHierarchy()
    {
        return FormUtilsClient.getRenderer(this.form) instanceof VanillaBoneMobFormRenderer renderer
            ? renderer.getBoneHierarchy()
            : BoneHierarchy.EMPTY;
    }

    @Override
    public void startEdit(MobForm form)
    {
        super.startEdit(form);

        MobFormValues values = MobFormValues.of(form);

        this.color.setColor(values.bbsvanillabone$getColor().get().getARGBColor());
        this.paused.setValue(values.bbsvanillabone$getPaused().get());
        this.slim.setValue(form.slim.get());
        this.mobNBT.setText(form.mobNBT.get());

        this.slim.removeFromParent();
        this.poseEditor.removeFromParent();
        this.nbtSection.removeFromParent();

        /* The slim toggle only applies to the player model — the tooltip already said so, now the
         * toggle follows: it appears only while a player mob is selected. */
        if (form.isPlayer())
        {
            this.options.add(this.slim);
        }

        this.options.add(this.poseEditor);
        /* NBT folds at the very bottom, under the bone editor. */
        this.options.add(this.nbtSection);
        this.options.resize();

        BoneHierarchy hierarchy = this.getBoneHierarchy();

        this.poseEditor.setValuePose(form.pose);
        this.poseEditor.setPose(form.pose.get(), "");
        this.poseEditor.fillGroups(new VanillaModel(hierarchy), hierarchy.buildFlippedParts(), true);
    }

    /**
     * Where a plain viewport click on a bone lands: the shell falls back to the default panel and
     * calls this, so selecting the bone here is what connects picking to the pose editor. Ctrl+click
     * takes the other route (UIVanillaBoneMobForm#toggleBoneSelection) to accumulate a selection.
     */
    @Override
    public void pickBone(String bone)
    {
        super.pickBone(bone);

        if (bone != null && !bone.isEmpty())
        {
            this.poseEditor.selectBone(bone);
        }
    }
}
