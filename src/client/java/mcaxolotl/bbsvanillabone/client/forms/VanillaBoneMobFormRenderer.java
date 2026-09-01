package mcaxolotl.bbsvanillabone.client.forms;

import mcaxolotl.bbsvanillabone.client.BBSVanillaBoneClientAddon;
import mcaxolotl.bbsvanillabone.client.bones.MobRenderContext;
import mcaxolotl.bbsvanillabone.client.bones.VanillaRendererBones;
import mcaxolotl.bbsvanillabone.client.mixin.MobFormRendererAccessor;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Stands in for the host's MobFormRenderer. The host looks its renderer up by
 * map.get(form.getClass()), so this is registered under MobForm.class itself rather than under a
 * form subclass — see BBSVanillaBoneClientAddon.
 *
 * NOTE for the next step: this still calls the host's render3D, which assigns
 * MobFormRenderer.currentPose and so leaves the host's LivingEntityRendererMixin applying
 * MobForm.pose to ModelPart fields. That is harmless only while the pose injected here comes from
 * somewhere else — as the test pose below does. The moment this starts injecting MobForm.pose
 * itself, the host's render3D has to go, or every rotation is applied twice and reads double.
 */
public class VanillaBoneMobFormRenderer extends MobFormRenderer
{
    /** TEMPORARY, for the pose-injection gate: bone id suffix to rotate, and by how much. */
    private static final String TEST_SUFFIX = System.getProperty("bbsvanillabone.testPose", "/head");
    private static final float TEST_YAW = Float.parseFloat(System.getProperty("bbsvanillabone.testPoseYaw", "45"));

    private static boolean announced;
    private static boolean reported;

    public VanillaBoneMobFormRenderer(MobForm form)
    {
        super(form);

        if (!announced)
        {
            announced = true;

            BBSVanillaBoneClientAddon.LOGGER.info("mob form renderer taken over: the host built a {}", this.getClass().getSimpleName());
        }
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        Object renderer = this.resolveVanillaRenderer();

        if (renderer == null)
        {
            super.render3D(context);

            return;
        }

        VanillaRendererBones.Discovery discovery = VanillaRendererBones.discover(renderer);

        try (MobRenderContext ignored = MobRenderContext.push(
            renderer,
            this.buildTestPose(discovery),
            new Pose(),
            new Color().set(Colors.WHITE)
        ))
        {
            super.render3D(context);
        }
    }

    /**
     * The vanilla renderer for the form's stand-in entity — the object the bone discovery walks for
     * model layers. The entity itself is the host's, reached through an accessor because the host
     * keeps it private and rebuilds it whenever the form's id or NBT changes.
     */
    private Object resolveVanillaRenderer()
    {
        MobFormRendererAccessor host = (MobFormRendererAccessor) this;

        host.bbsvanillabone$ensureEntity();

        Entity entity = host.bbsvanillabone$getEntity();

        return entity == null ? null : MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
    }

    /** TEMPORARY. Rotates every discovered bone whose id ends with TEST_SUFFIX. */
    private Pose buildTestPose(VanillaRendererBones.Discovery discovery)
    {
        Pose pose = new Pose();

        for (String id : discovery.getBoneIds())
        {
            if (id.endsWith(TEST_SUFFIX))
            {
                PoseTransform transform = pose.get(id);

                transform.rotate.y = (float) Math.toRadians(TEST_YAW);
            }
        }

        if (!reported)
        {
            reported = true;

            BBSVanillaBoneClientAddon.LOGGER.info(
                "test pose: {} of {} discovered bones match \"{}\", rotating {} degrees on yaw -> {}",
                pose.transforms.size(), discovery.getBoneIds().size(), TEST_SUFFIX, TEST_YAW, pose.transforms.keySet()
            );
        }

        return pose;
    }
}
