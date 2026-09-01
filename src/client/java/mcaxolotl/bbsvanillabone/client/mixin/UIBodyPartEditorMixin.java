package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.bones.VanillaModel;
import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.UIBodyPartEditor;
import mchorse.bbs_mod.ui.utils.bones.UIBonePickerContextMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * The attachment-bone control in the body part editor: both the button that names the bone and the
 * picker popup it opens.
 *
 * <p>The button names the bone a nested form hangs off, and for a vanilla bone that name was the
 * whole stable id — {@code minecraft:zombie#main/head} in a button sized for {@code head}. The id
 * still travels in the data; only what the button reads changes.</p>
 */
@Mixin(value = UIBodyPartEditor.class, remap = false)
public class UIBodyPartEditorMixin
{
    /* The parent form the part hangs off, which is whose bone the button names — not the form being
     * edited inside the part. */
    @Shadow
    private Form owner;

    /* The part being edited, whose bone is the picker's current value. */
    @Shadow
    private BodyPart part;

    /** The button's own text. A bone the hierarchy does not know keeps the host's raw id. */
    @Inject(method = "boneLabel(Ljava/lang/String;)Lmchorse/bbs_mod/l10n/keys/IKey;", at = @At("HEAD"), cancellable = true)
    private void bbsvanillabone$mobBoneLabel(String bone, CallbackInfoReturnable<IKey> info)
    {
        if (bone == null || bone.isEmpty() || !(this.owner instanceof MobForm mobForm))
        {
            return;
        }

        BoneHierarchy hierarchy = VanillaBoneTrackSheets.getBoneHierarchy(mobForm);
        String label = hierarchy == null ? null : hierarchy.getLabel(bone);

        if (label != null)
        {
            info.setReturnValue(IKey.constant(label));
        }
    }

    /**
     * The picker popup behind that button, for a part whose parent is a mob form.
     *
     * <p>The host fills it from {@code FormUtilsClient.getBones(owner)} through
     * {@code UIBonePickerContextMenu.list}, i.e. {@code UIBoneTreeList.fillFlat}, which adds the
     * plain strings and nothing else — no row objects, no metadata — so neither the hierarchy nor
     * the short names can reach it: a zombie opens ~30 rows of
     * {@code minecraft:zombie#inner_armor/right_leg} in flat alphabetical order. Handing the same
     * bones to the model overload instead gives the popup the tree the pose editor already draws,
     * and the short names come with it from UIBoneTreeListMixin, which relabels every row that walk
     * builds.</p>
     *
     * <p>Nothing about the value changes: the rows still carry stable bone ids, which is what
     * {@code BodyPart.bone} goes on storing. The wrapper defers to the host for everything that is
     * not a mob form with a resolvable hierarchy, including the world-less case where the hierarchy
     * cannot be built, so the model form path is byte for byte the host's.</p>
     *
     * <p>Wrapping the argument at the {@code menu(...)} call is what keeps this off a synthetic
     * lambda. The host's configurator is a {@code lambda$new$N} whose name carries a compile-time
     * counter, so a mixin aimed at it would break the moment the host adds or removes a lambda above
     * it; the {@code menu(...)} call it is passed to is an ordinary constructor instruction. The
     * configurator is a setter with a private backing field, so wrapping the argument is also the
     * only way to keep the host's own fill as the fallback.</p>
     */
    @ModifyArg(
        method = "<init>(Lmchorse/bbs_mod/ui/forms/editors/UIFormEditor;)V",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/utils/bones/UIBonePicker;menu(Ljava/util/function/Consumer;)Lmchorse/bbs_mod/ui/utils/bones/UIBonePicker;"
        ),
        index = 0
    )
    private Consumer<UIBonePickerContextMenu> bbsvanillabone$mobBoneMenu(Consumer<UIBonePickerContextMenu> configurator)
    {
        return (picker) ->
        {
            BoneHierarchy hierarchy = this.part != null && this.owner instanceof MobForm mobForm
                ? VanillaBoneTrackSheets.getBoneHierarchy(mobForm)
                : null;

            if (hierarchy == null)
            {
                configurator.accept(picker);

                return;
            }

            /* No hidden set: the disabled-bone marks the host filters on are a bbs model's, and a
             * vanilla model carries none. */
            picker.bones(new VanillaModel(hierarchy), null).none().set(this.part.bone.get());
        };
    }
}
