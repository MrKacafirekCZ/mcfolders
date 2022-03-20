package mrkacafirekcz.mcfolders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import mrkacafirekcz.mcfolders.config.JsonConfig;
import mrkacafirekcz.mcfolders.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;

public class MCFoldersMod implements ModInitializer {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(References.MODID);
	public static ModConfig CONFIG;
	
	@Override
	public void onInitialize() {
		loadConfig();
		
		if(CONFIG == null) {
			CONFIG = new ModConfig();
			CONFIG.directoryResourcePack = References.DEFAULT_MC_RESOURCE_PACK_FOLDER;
			CONFIG.directorySaves = References.DEFAULT_MC_SAVES_FOLDER;
			saveConfig();
		}
	}
	
	public static Screen getConfigMenu(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(new LiteralText("MCFolders config"));
		builder.setSavingRunnable(() -> {
			saveConfig();
		});
		
		ConfigCategory general = builder.getOrCreateCategory(new LiteralText("General"));
		
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		
		general.addEntry(entryBuilder.startStrField(new LiteralText("Resource pack directory"), CONFIG.directoryResourcePack)
				.setDefaultValue(References.DEFAULT_MC_RESOURCE_PACK_FOLDER)
				.setSaveConsumer(newValue -> CONFIG.directoryResourcePack = newValue)
				.build());

		general.addEntry(entryBuilder.startStrField(new LiteralText("Saves directory"), CONFIG.directorySaves)
				.setDefaultValue(References.DEFAULT_MC_SAVES_FOLDER)
				.setSaveConsumer(newValue -> CONFIG.directorySaves = newValue)
				.build());
		
		return builder.build();
	}
	
	public static void loadConfig() {
		JsonConfig config = new JsonConfig("mcfolders");
		config.readJson();
		Gson gson = new Gson();
		CONFIG = gson.fromJson(config.getJson(), ModConfig.class);
	}
	
	public static void saveConfig() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonConfig config = new JsonConfig("mcfolders");
		config.setJson(gson.toJson(CONFIG));
		config.writeJson();
	}
}
