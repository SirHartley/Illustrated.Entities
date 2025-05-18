package illustratedEntities.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;

public class UnusedImageUnloader implements EconomyTickListener {

    public static void register(){
        Global.getSector().getListenerManager().addListener(new UnusedImageUnloader(), true);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        for (ImageDataEntry entry : ImageDataMemory.getInstance().getDataMap().values()) if (!entry.isUsed()) entry.unload();
    }

    @Override
    public void reportEconomyMonthEnd() {

    }
}
