package mrkacafirekcz.mcfolders.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mrkacafirekcz.mcfolders.MCFoldersMod;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;

@Mixin(WorldListWidget.class)
public class WorldListWidgetMixin extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry> {

	@Shadow
    private List<LevelSummary> levels;

	public WorldListWidgetMixin() {
		super(null, 0, 0, 0, 0, 0);
	}

	@Inject(at = @At("HEAD"), method = "filter(Ljava/util/function/Supplier;Z)V", cancellable = true)
	private void filter(Supplier<String> searchTextSupplier, boolean load, CallbackInfo info) {
		info.cancel();
		
        this.clearEntries();
        LevelStorage levelStorage = this.client.getLevelStorage();
        if (this.levels == null || load) {
            try {
                this.levels = levelStorage.getLevelList();
            }
            catch (LevelStorageException levelStorageException) {
                MCFoldersMod.LOGGER.error("Couldn't load level list", levelStorageException);
                this.client.setScreen(new FatalErrorScreen(new TranslatableText("selectWorld.unable_to_load"), new LiteralText(levelStorageException.getMessage())));
                return;
            }
            Collections.sort(this.levels);
        }

        WorldListWidget instance = ((WorldListWidget) (Object) this);
        
        String string = searchTextSupplier.get().toLowerCase(Locale.ROOT);
        for (LevelSummary levelSummary : this.levels) {
            if (!levelSummary.getDisplayName().toLowerCase(Locale.ROOT).contains(string) && !levelSummary.getName().toLowerCase(Locale.ROOT).contains(string)) continue;
            this.addEntry(instance.new Entry(instance, levelSummary));
        }
    }
}
