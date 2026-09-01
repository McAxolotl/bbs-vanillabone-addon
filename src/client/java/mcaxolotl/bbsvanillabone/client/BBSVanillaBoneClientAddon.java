package mcaxolotl.bbsvanillabone.client;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client half of the addon, picked up from the "bbs-client-addon" entrypoint. The host scans that
 * entrypoint at the very top of its own client init, so everything registered from a subscriber
 * here lands before any of the host's own posts — no Fabric entrypoint ordering involved.
 */
public class BBSVanillaBoneClientAddon implements BBSAddonMod
{
    public static final Logger LOGGER = LoggerFactory.getLogger("bbsvanillabone");

    /**
     * Registration point for the form renderer and editor panel overrides. Both registries are
     * plain static maps, so the only real constraint is being early enough that no mob form has
     * been rendered yet (the host caches a renderer back into the form instance) — this event is
     * posted during the host's client init, far ahead of that.
     */
    @Subscribe
    public void onRegisterClientSettings(RegisterClientSettingsEvent event)
    {
        LOGGER.info("bbs-vanillabone-addon: client addon attached, host event bus reached");
    }
}
