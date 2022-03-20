package mrkacafirekcz.mcfolders.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import mrkacafirekcz.mcfolders.MCFoldersMod;

public class ModMenuIntegration implements ModMenuApi {
	
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			return MCFoldersMod.getConfigMenu(parent);
		};
	}
}
