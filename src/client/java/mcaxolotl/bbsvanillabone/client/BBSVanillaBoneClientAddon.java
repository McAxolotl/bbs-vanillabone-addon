package mcaxolotl.bbsvanillabone.client;

import mcaxolotl.bbsvanillabone.BBSVanillaBoneAddon;
import mcaxolotl.bbsvanillabone.client.forms.VanillaBoneMobFormRenderer;
import mcaxolotl.bbsvanillabone.client.ui.UIVanillaBoneMobForm;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Client half of the addon, picked up from the "bbs-client-addon" entrypoint. The host scans that
 * entrypoint at the very top of its own client init, so everything registered from a subscriber
 * here lands before any of the host's own posts — no Fabric entrypoint ordering involved.
 */
public class BBSVanillaBoneClientAddon implements BBSAddonMod
{
    public static final Logger LOGGER = LoggerFactory.getLogger("bbsvanillabone");

    /**
     * The languages this addon ships strings for. Asked for anything else it hands back nothing:
     * the host's loader reports a link it cannot resolve with a stack trace, once per language
     * reload, and a missing translation is not worth one.
     */
    private static final Set<String> LANGUAGES = Set.of(
        "ar_ar",
        "de_de",
        "en_us",
        "es_es",
        "fr_fr",
        "hu_hu",
        "id_id",
        "ko_kr",
        "pl_pl",
        "pt_br",
        "pt_pt",
        "ru_ru",
        "th_th",
        "tr_tr",
        "uk_ua",
        "ur_pk",
        "vi_vn",
        "zh_cn",
        "zh_tw"
    );

    /**
     * The host posts this before it loads its own language files, precisely so an addon's labels are
     * translated on first sight rather than showing their raw key until the next language switch.
     *
     * The links resolve against the addon's own asset source, registered by the common half — the
     * host's "assets:" source already has a strings/en_us.json of its own to collide with.
     */
    @Subscribe
    public void onRegisterL10n(RegisterL10nEvent event)
    {
        event.l10n.register(BBSVanillaBoneClientAddon::langFiles);
    }

    private static List<Link> langFiles(String lang)
    {
        return LANGUAGES.contains(lang)
            ? Collections.singletonList(new Link(BBSVanillaBoneAddon.MOD_ID, "strings/" + lang + ".json"))
            : Collections.emptyList();
    }

    /**
     * Both registries are plain static maps keyed on the form class, so taking mob forms over is a
     * pair of put calls. Two things make the timing matter rather than the mechanism:
     *
     * The key has to be MobForm.class literally. The host resolves a renderer with
     * map.get(form.getClass()), not an instanceof walk, so registering under a form subclass would
     * simply never be found.
     *
     * It has to happen before the first mob form renders, because the host caches the renderer it
     * built back onto the form instance. This event is posted from inside the host's client init,
     * which is far ahead of any rendering — and, unlike doing this from the addon's own
     * onInitializeClient, it means touching FormUtilsClient (whose static block eagerly allocates a
     * BlockBufferBuilderStorage and a batch of BufferBuilders on 1.20.x) happens at a point where
     * the GL context and the host are already up.
     */
    @Subscribe
    public void onRegisterClientSettings(RegisterClientSettingsEvent event)
    {
        LOGGER.info("bbs-vanillabone-addon: client addon attached, host event bus reached");

        FormUtilsClient.register(MobForm.class, VanillaBoneMobFormRenderer::new);
        UIFormEditor.register(MobForm.class, UIVanillaBoneMobForm::new);

        LOGGER.info("bbs-vanillabone-addon: mob form renderer and editor panel registered over the host's");
    }
}
