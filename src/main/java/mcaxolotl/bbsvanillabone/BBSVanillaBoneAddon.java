package mcaxolotl.bbsvanillabone;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;

/**
 * Common half of the addon, picked up from the "bbs-addon" entrypoint. Everything this addon
 * actually does is client side; this half exists for the one event that is posted from the host's
 * common initialization, before the client entrypoint has even been scanned — so a subscriber
 * living in the client half could never receive it.
 */
public class BBSVanillaBoneAddon implements BBSAddonMod
{
    public static final String MOD_ID = "bbsvanillabone";

    /**
     * Gives this addon's own assets a source of their own, so they can be addressed as
     * {@code bbsvanillabone:...} links. Sharing the host's {@code assets:} source instead would mean
     * fighting over file names with it — and {@code strings/en_us.json} is a name it already uses.
     */
    @Subscribe
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon(MOD_ID, BBSVanillaBoneAddon.class);
    }
}
