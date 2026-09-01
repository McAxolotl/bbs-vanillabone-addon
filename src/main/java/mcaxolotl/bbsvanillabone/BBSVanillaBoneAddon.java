package mcaxolotl.bbsvanillabone;

import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;

/**
 * Common half of the addon, picked up from the "bbs-addon" entrypoint. The bone editing itself is
 * client side, but two pieces cannot be: the event below is posted from the host's common
 * initialization, before the client entrypoint has even been scanned, so a subscriber living in the
 * client half could never receive it — and MobFormMixin has to apply on a dedicated server as well,
 * or the server strips this addon's form values out of every form it round trips.
 *
 * <p>Which is why the mod's environment is "*". Nothing in this source set touches a client class;
 * the split source sets make that structural rather than a convention, since the host's client
 * classes are not even on this half's compile classpath.</p>
 */
public class BBSVanillaBoneAddon implements BBSAddonMod
{
    public static final String MOD_ID = "bbsvanillabone";

    /**
     * Gives this addon's own assets a source of their own, so they can be addressed as
     * {@code bbsvanillabone:...} links. Sharing the host's {@code assets:} source instead would mean
     * fighting over file names with it — and {@code strings/en_us.json} is a name it already uses.
     *
     * <p>Posted from the host's common initialization, so this also runs on a dedicated server,
     * where it registers a source nothing ever reads (only the client's l10n resolves these links).
     * Registering it is a map insert on a common-side AssetProvider either way.</p>
     */
    @Subscribe
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon(MOD_ID, BBSVanillaBoneAddon.class);
    }
}
