package mrkacafirekcz.mcfolders.mixin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mrkacafirekcz.mcfolders.MCFoldersMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.FileResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

@Mixin(PackScreen.class)
public class PackScreenMixin extends Screen {

	@Shadow
	static final Text FOLDER_INFO = new TranslatableText("pack.folderInfo");
	@Shadow
    private PackListWidget availablePackList;
	@Shadow
    private ButtonWidget doneButton;
	@Shadow
    private final File file;
	@Shadow
    private final ResourcePackOrganizer organizer;
	@Shadow
    private final Screen parent;
	@Shadow
    private PackListWidget selectedPackList;
	
	private boolean overrideScreen;
	private TextFieldWidget resourcePackFolderField;
	private boolean resourcePackFolderValid;
	private static final Text RESOURCE_PACK_FOLDER = new LiteralText("Resource pack directory");
	private static final Text INVALID_RESOURCE_PACK_FOLDER = new LiteralText("Invalid resource pack directory");
	private static final Text VALID_RESOURCE_PACK_FOLDER = new LiteralText("Valid resource pack directory");
	
	protected PackScreenMixin() {
		super(new LiteralText("Invalid constructor call!"));
        this.file = null;
        this.organizer = null;
        this.parent = null;
	}
	
	@Shadow
    private void closeDirectoryWatcher() {
    	// PackScreen#closeDirectoryWatcher();
    }

	@Inject(at = @At("HEAD"), method = "init()V", cancellable = true)
	private void init(CallbackInfo info) {
		overrideScreen = this.parent instanceof OptionsScreen;
		
		if(overrideScreen) {
			info.cancel();
			
	        this.client.keyboard.setRepeatEvents(true);
	        
	        this.resourcePackFolderField = new TextFieldWidget(this.textRenderer, this.width / 2 - 204, 50, 408, 20, new LiteralText(""));
	        this.resourcePackFolderField.setMaxLength(1024);
	        this.resourcePackFolderField.setText(MCFoldersMod.CONFIG.directoryResourcePack);
	        this.resourcePackFolderField.setChangedListener(resourcePackFolder -> {
	        	if(MCFoldersMod.CONFIG.directoryResourcePack != resourcePackFolder) {
	        		MCFoldersMod.CONFIG.directoryResourcePack = resourcePackFolder;
	            	this.updateResourcePackFolder();
	        	}
	        });
	        this.addDrawableChild(this.resourcePackFolderField);
			this.addDrawableChild(new ButtonWidget(this.width / 2 - 54, this.height - 48, 100, 20, new LiteralText("Configuration"), button -> {
				this.client.setScreen(MCFoldersMod.getConfigMenu(this));
			}));
			
			this.doneButton = this.addDrawableChild(new ButtonWidget(this.width / 2 + 50, this.height - 48, 100, 20, ScreenTexts.DONE, button -> this.close()));
	        this.addDrawableChild(new ButtonWidget(this.width / 2 - 158, this.height - 48, 100, 20, new TranslatableText("pack.openFolder"), button -> Util.getOperatingSystem().open(this.file), new ButtonWidget.TooltipSupplier(){

	            @Override
	            public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
	            	renderTooltip(matrixStack, FOLDER_INFO, i, j);
	            }

	            @Override
	            public void supply(Consumer<Text> consumer) {
	                consumer.accept(FOLDER_INFO);
	            }
	        }));
	        this.availablePackList = new PackListWidget(this.client, 200, this.height, new TranslatableText("pack.available.title"));
	        this.availablePackList.updateSize(200, this.height, 90, this.height - 60);
	        this.availablePackList.setLeftPos(this.width / 2 - 4 - 200);
	        this.addSelectableChild(this.availablePackList);
	        this.selectedPackList = new PackListWidget(this.client, 200, this.height, new TranslatableText("pack.selected.title"));
	        this.selectedPackList.updateSize(200, this.height, 90, this.height - 60);
	        this.selectedPackList.setLeftPos(this.width / 2 + 4);
	        this.addSelectableChild(this.selectedPackList);
			this.refresh();
			this.updateResourcePackFolder();
		}
	}

	@Shadow
    private void refresh() {
    	// PackScreen#refresh();
    }

    @Override
    public void removed() {
    	if(overrideScreen) {
            this.client.keyboard.setRepeatEvents(false);
    	}
    }

	@Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V")
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
    	if(overrideScreen) {
    		PackScreen.drawTextWithShadow(matrices, this.textRenderer, RESOURCE_PACK_FOLDER, this.width / 2 - 204, 37, 0xFFFFFF);
        	
        	if(this.resourcePackFolderValid) {
        		PackScreen.drawTextWithShadow(matrices, this.textRenderer, VALID_RESOURCE_PACK_FOLDER, this.width / 2 - 204, 75, 0x00FF00);
        	} else {
        		PackScreen.drawTextWithShadow(matrices, this.textRenderer, INVALID_RESOURCE_PACK_FOLDER, this.width / 2 - 204, 75, 0xFF0000);
        	}
    	}
    }

	@Inject(at = @At("HEAD"), method = "tick()V")
    private void tick(CallbackInfo info) {
    	if(overrideScreen) {
            this.resourcePackFolderField.tick();
    	}
    }
	
	private void updateResourcePackFolder() {
		File newResourcePackFolder = new File(MCFoldersMod.CONFIG.directoryResourcePack);
		
		this.resourcePackFolderValid = newResourcePackFolder.exists() && newResourcePackFolder.isDirectory();
		
		if(this.resourcePackFolderValid) {
			try {
				// Update file
				Field[] fields = PackScreen.class.getDeclaredFields();
				Field fileField = null;
				
				for(Field field : fields) {
					if(field.getType() == File.class) {
						fileField = field;
						break;
					}
				}
				
				if(fileField != null) {
					fileField.setAccessible(true);
					fileField.set(this, newResourcePackFolder);
					fileField.setAccessible(false);
				}
				
				// Update organizer
				Field[] fields2 = ResourcePackOrganizer.class.getDeclaredFields();
				Field managerField = null;
				
				for(Field field : fields2) {
					if(field.getType() == ResourcePackManager.class) {
						managerField = field;
					}
				}
				
				if(managerField != null) {
					managerField.setAccessible(true);
					ResourcePackManager manager = (ResourcePackManager) managerField.get(this.organizer);
					managerField.setAccessible(false);
					
					Field[] fields3 = ResourcePackManager.class.getDeclaredFields();
					Field providersField = null;
					
					for(Field field : fields3) {
						if(field.getType() == Set.class) {
							providersField = field;
							MCFoldersMod.LOGGER.info("Found providers field!");
							break;
						}
					}
					
					if(providersField != null) {
						providersField.setAccessible(true);
						@SuppressWarnings("unchecked")
						Set<ResourcePackProvider> providers = (Set<ResourcePackProvider>) providersField.get(manager);
						providersField.setAccessible(false);
						
						providers.clear();
						providers.add(MinecraftClient.getInstance().getResourcePackProvider());
						providers.add(new FileResourcePackProvider(newResourcePackFolder, ResourcePackSource.PACK_SOURCE_NONE));
					}
				}

				// Update directory watcher
				Class<?>[] classes = PackScreen.class.getClasses();
				Class<?> directoryWatcherClass = classes[0]; // Pray to god that it's always the first class in the list!
				Field directoryWatcherField = null;
				
				for(Field field : fields) {
					if(field.getType() == directoryWatcherClass) {
						directoryWatcherField = field;
						break;
					}
				}
				
				if(directoryWatcherField != null) {
					Method[] methods = directoryWatcherClass.getDeclaredMethods();
					Method closeMethod = null;
					
					for(Method method : methods) {
						// I hope this works well too
						if(method.getReturnType().getName().equals("void")  && method.getParameterCount() == 0) {
							closeMethod = method;
							break;
						}
					}
					
					if(closeMethod != null) {
						closeMethod.invoke(directoryWatcherField.get(this));
						
						directoryWatcherField.setAccessible(true);
						directoryWatcherField.set(this, directoryWatcherClass.getConstructor(File.class).newInstance(newResourcePackFolder));
						directoryWatcherField.setAccessible(false);
					}
				}
				
				fields = MinecraftClient.class.getDeclaredFields();
				Field rpFileField = null;
				
				for(Field field : fields) {
					// Pray to god it's the first one
					if(field.getType() == File.class) {
						rpFileField = field;
						break;
					}
				}

				if(rpFileField != null) {
					rpFileField.setAccessible(true);
					rpFileField.set(MinecraftClient.getInstance(), newResourcePackFolder);
					rpFileField.setAccessible(false);
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			
			this.refresh();
		}
	}
}
