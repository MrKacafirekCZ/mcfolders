package mrkacafirekcz.mcfolders.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import mrkacafirekcz.mcfolders.MCFoldersMod;
import net.minecraft.client.MinecraftClient;

public class JsonConfig {
	
	private String filename;
	private File file;
	private String json;
	
	public JsonConfig(String filename) {
		this.filename = filename.endsWith(".json") ? filename.substring(0, filename.length() - 5) : filename;
	}
	
	public boolean exists() {
		File file = new File(getDirectory(), filename + ".json");
		
		return file.exists();
	}
	
	public File getDirectory() {
		File configDir = new File(MinecraftClient.getInstance().runDirectory + "/config/");
		
		if(!configDir.exists()) {
			configDir.mkdirs();
		}
		
		return configDir;
	}
	
	private File getFile() {
		File file = new File(getDirectory(), filename + ".json");
		
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				MCFoldersMod.LOGGER.error("Error while creating a json file!");
				e.printStackTrace();
			}
		}
		
		return file;
	}
	
	public String getJson() {
		return json;
	}
	
	public void readJson() {
		if(!exists()) {
			return;
		}
		
		if(this.file == null) {
			this.file = getFile();
		}
		
		try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
			StringBuilder sb = new StringBuilder();
		    String line = reader.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = reader.readLine();
		    }
		    
			json = sb.toString();
		} catch (IOException e) {
			MCFoldersMod.LOGGER.error("Error while reading a json file!");
			e.printStackTrace();
		}
	}
	
	public void setJson(String json) {
		this.json = json;
	}
	
	public void writeJson() {
		if(this.file == null) {
			this.file = getFile();
		}
		
		try(FileWriter writer = new FileWriter(file)) {
			writer.write(json);
			writer.flush();
		} catch (IOException e) {
			MCFoldersMod.LOGGER.error("Error while writing a json file!");
			e.printStackTrace();
		}
	}
}
