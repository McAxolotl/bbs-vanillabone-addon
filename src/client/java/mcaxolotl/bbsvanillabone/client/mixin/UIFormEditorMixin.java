package mcaxolotl.bbsvanillabone.client.mixin;

import mcaxolotl.bbsvanillabone.client.ui.UIVanillaBoneMobForm;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies the gizmo's additive rotation base for mob forms.
 *
 * <p>The editor shell asks the active form editor for that base, but through a private method whose
 * type gate is written as {@code instanceof UIModelForm} — so any other form editor, including this
 * addon's, is handed null and the drag's base collapses to zero. Neither inheritance nor an override
 * gets past a private method with a hardcoded type check, which makes this the one place the addon
 * has to inject.</p>
 *
 * <p>It is not cosmetic for vanilla bones: vanilla setAngles writes non-zero angles to heads and
 * limbs every frame, so a zero base offsets every rotation drag by whatever the animation is doing.
 * The pose editing itself works without this; only rotation dragging is wrong.</p>
 *
 * <p>The forwarding target computes the base from public API only (see
 * UIVanillaBoneMobForm#poseRotationBase), keeping the injected code to a type check and a return.
 * The host has no mixins of its own on this class.</p>
 */
@Mixin(value = UIFormEditor.class, remap = false)
public class UIFormEditorMixin
{
    @Inject(
        method = "poseRotationBase(Lmchorse/bbs_mod/ui/framework/elements/input/UIPropTransform;F)Lorg/joml/Vector3f;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbsvanillabone$mobFormRotationBase(UIPropTransform transform, float transition, CallbackInfoReturnable<Vector3f> info)
    {
        UIFormEditor editor = (UIFormEditor) (Object) this;

        if (!(editor.editor instanceof UIVanillaBoneMobForm mobForm))
        {
            return;
        }

        /* The same two gates the host applies before it asks a form editor at all: the body part
         * transform and the state transforms are not pose-stacked. Left uncancelled rather than
         * answered with null, so the host's own return keeps being the one that stands. */
        if (editor.isBodyPartGizmoMode() || editor.statesEditor.isVisible())
        {
            return;
        }

        info.setReturnValue(mobForm.poseRotationBase(transform, transition));
    }
}
