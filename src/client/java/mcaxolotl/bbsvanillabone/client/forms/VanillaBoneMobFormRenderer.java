package mcaxolotl.bbsvanillabone.client.forms;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;

/**
 * Stands in for the host's MobFormRenderer. The host looks its renderer up by
 * map.get(form.getClass()), so this is registered under MobForm.class itself rather than under a
 * form subclass — see BBSVanillaBoneClientAddon.
 *
 * Inherits everything for now, which is the point at this step: rendering has to stay identical to
 * a game without the addon, so that the takeover itself is what is being observed.
 *
 * NOTE for the pose-injection step: this must stop inheriting the host's render3D. The host's
 * LivingEntityRendererMixin applies MobForm.pose to ModelPart fields whenever
 * MobFormRenderer.getCurrentPose() is non-null, and currentPose is assigned only inside the host's
 * render3D. Once this addon injects the same pose through ModelPart.rotate, leaving render3D
 * inherited applies it twice and every rotation reads double.
 */
public class VanillaBoneMobFormRenderer extends MobFormRenderer
{
    private static boolean announced;

    public VanillaBoneMobFormRenderer(MobForm form)
    {
        super(form);

        if (!announced)
        {
            announced = true;

            BBSVanillaBoneClientAddon.LOGGER.info("mob form renderer taken over: the host built a {}", this.getClass().getSimpleName());
        }
    }
}
