package mcaxolotl.bbsvanillabone.client.ui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;

/**
 * The labels this addon adds on top of the host's UIKeys. Same shape as UIKeys: L10n.lang binds a
 * key that the addon's own language files fill in, so the text follows the language setting instead
 * of being frozen in English the way IKey.raw would be.
 *
 * <p>The key names are bbs-fsv's, so a translation written against either project covers both.</p>
 */
public class VanillaBoneKeys
{
    public static final IKey FORMS_EDITORS_MOB_MOBS = L10n.lang("bbs.ui.forms.editors.mob.mobs");
    public static final IKey FORMS_EDITORS_MOB_PICK_MOB = L10n.lang("bbs.ui.forms.editors.mob.pick_mob");
}
