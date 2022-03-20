package mrkacafirekcz.mcfolders.mixin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mrkacafirekcz.mcfolders.MCFoldersMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(SelectWorldScreen.class)
public class SelectWorldScreenMixin extends Screen {

	@Shadow
    private ButtonWidget deleteButton;
	@Shadow
    private ButtonWidget selectButton;
	@Shadow
    private ButtonWidget editButton;
	@Shadow
    private ButtonWidget recreateButton;
	@Shadow
    protected TextFieldWidget searchBox;
	@Shadow
    private WorldListWidget levelList;
	@Shadow
    protected final Screen parent;
    
	private TextFieldWidget savesFolderField;
	private boolean savesFolderValid;
	private static final Text SAVES_FOLDER = new LiteralText("Saves directory");
	private static final Text INVALID_SAVES_FOLDER = new LiteralText("Invalid saves directory");
	private static final Text VALID_SAVES_FOLDER = new LiteralText("Valid saves directory");

	protected SelectWorldScreenMixin() {
		super(new LiteralText("Invalid constructor call!"));
        this.parent = null;
	}


	@Inject(at = @At("HEAD"), method = "charTyped(CI)Z", cancellable = true)
    public boolean charTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> info) {
		info.cancel();
		
		if (this.searchBox.isActive() && this.searchBox.charTyped(chr, modifiers)) {
        	info.setReturnValue(true);
        	return true;
        }

    	info.setReturnValue(this.savesFolderField.charTyped(chr, modifiers));
        return info.getReturnValueZ();
    }

	@Inject(at = @At("HEAD"), method = "init()V", cancellable = true)
	private void init(CallbackInfo info) {
		info.cancel();
		
        this.client.keyboard.setRepeatEvents(true);
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, this.searchBox, new TranslatableText("selectWorld.search"));
        this.searchBox.setChangedListener(search -> this.levelList.filter(() -> search, false));
        try {
			this.levelList = WorldListWidget.class.getConstructor(SelectWorldScreen.class, MinecraftClient.class, int.class, int.class, int.class, int.class, int.class, Supplier.class, WorldListWidget.class)
					.newInstance(this, this.client, this.width, this.height, 100, this.height - 64, 36, test(), this.levelList);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
        this.addSelectableChild(this.searchBox);
        this.addSelectableChild(this.levelList);
        
        this.savesFolderField = new TextFieldWidget(this.textRenderer, this.width / 2 - 204, 60, 408, 20, new LiteralText(""));
        this.savesFolderField.setMaxLength(1024);
        this.savesFolderField.setText(MCFoldersMod.CONFIG.directorySaves);
        this.savesFolderField.setChangedListener(savesFolder -> {
        	if(MCFoldersMod.CONFIG.directorySaves != savesFolder) {
        		MCFoldersMod.CONFIG.directorySaves = savesFolder;
            	this.updateSavesFolder();
        	}
        });
        this.addDrawableChild(this.savesFolderField);
        
        this.selectButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 154, this.height - 52, 110, 20, new TranslatableText("selectWorld.select"), button -> this.levelList.getSelectedAsOptional().ifPresent(WorldListWidget.Entry::play)));
        
		this.addDrawableChild(new ButtonWidget(this.width / 2 - 40, this.height - 52, 80, 20, new LiteralText("Configuration"), button -> {
			this.client.setScreen(MCFoldersMod.getConfigMenu(this));
		}));
        
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 44, this.height - 52, 110, 20, new TranslatableText("selectWorld.create"), button -> this.client.setScreen(CreateWorldScreen.create(this))));
        this.editButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 154, this.height - 28, 72, 20, new TranslatableText("selectWorld.edit"), button -> this.levelList.getSelectedAsOptional().ifPresent(WorldListWidget.Entry::edit)));
        this.deleteButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 76, this.height - 28, 72, 20, new TranslatableText("selectWorld.delete"), button -> this.levelList.getSelectedAsOptional().ifPresent(WorldListWidget.Entry::deleteIfConfirmed)));
        this.recreateButton = this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height - 28, 72, 20, new TranslatableText("selectWorld.recreate"), button -> this.levelList.getSelectedAsOptional().ifPresent(WorldListWidget.Entry::recreate)));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 82, this.height - 28, 72, 20, ScreenTexts.CANCEL, button -> this.client.setScreen(this.parent)));
        this.worldSelected(false);
        this.setInitialFocus(this.searchBox);
    	this.updateSavesFolder();
	}

	@Inject(at = @At("HEAD"), method = "keyPressed(III)Z", cancellable = true)
	public boolean keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
		info.cancel();
		
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
        	info.setReturnValue(true);
            return true;
        }
        
        if (this.searchBox.isActive() && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
        	info.setReturnValue(true);
        	return true;
        }

    	info.setReturnValue(this.savesFolderField.keyPressed(keyCode, scanCode, modifiers));
        return info.getReturnValueZ();
    }

	@Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V")
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
		PackScreen.drawTextWithShadow(matrices, this.textRenderer, SAVES_FOLDER, this.width / 2 - 204, 47, 0xFFFFFF);
    	
    	if(this.savesFolderValid) {
    		PackScreen.drawTextWithShadow(matrices, this.textRenderer, VALID_SAVES_FOLDER, this.width / 2 - 204, 85, 0x00FF00);
    	} else {
    		PackScreen.drawTextWithShadow(matrices, this.textRenderer, INVALID_SAVES_FOLDER, this.width / 2 - 204, 85, 0xFF0000);
    	}
    }
	
	@SuppressWarnings("rawtypes") // If it ain't broken, don't fix it
	private Supplier test() {
		return () -> this.searchBox.getText();
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
    private void tick(CallbackInfo info) {
        this.savesFolderField.tick();
    }
	
	@SuppressWarnings("unchecked")
	private void updateSavesFolder() {
		File newSavesFolder = new File(MCFoldersMod.CONFIG.directorySaves);
		
		this.savesFolderValid = newSavesFolder.exists() && newSavesFolder.isDirectory();
		
		if(this.savesFolderValid) {
			Field[] fields = LevelStorage.class.getDeclaredFields();
			Field savesDirectoryField = null;
			
			for(Field field : fields) {
				if(field.getType() == Path.class) {
					savesDirectoryField = field;
					MCFoldersMod.LOGGER.info("Found saves directory field");
					break;
				}
			}
			
			if(savesDirectoryField != null) {
				try {
					savesDirectoryField.setAccessible(true);
					savesDirectoryField.set(MinecraftClient.getInstance().getLevelStorage(), newSavesFolder.toPath());
					savesDirectoryField.setAccessible(false);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				MCFoldersMod.LOGGER.info("Didn't find saves directory field");
			}
			
			this.levelList.filter(test(), true);
		}
	}

	@Shadow
    public void worldSelected(boolean active) {
    	//SelectWorldScreen#worldSelected();
    }
}
