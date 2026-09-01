package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.bones.BoneHierarchy;
import mcaxolotl.bbsvanillabone.client.film.VanillaBoneTrackSheets;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.UIBodyPartEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The attachment-bone button in the body part editor.
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
}
