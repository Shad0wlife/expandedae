package lu.kolja.expandedae.datagen;

import lu.kolja.expandedae.Expandedae;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Expandedae.MODID, value = Dist.CLIENT)
public class ExpDataGen {
    @SubscribeEvent
    public static void datagen(GatherDataEvent event) {
        var gen = event.getGenerator();

        var output = gen.getPackOutput();
        gen.addProvider(event.includeClient(), new ExpLangProvider(output));

        var existing = event.getExistingFileHelper();
        gen.addProvider(event.includeClient(), new ExpModelProvider(output, existing));

        var registries = event.getLookupProvider();
        gen.addProvider(event.includeServer(), new ExpRecipeProvider(output, registries));
        gen.addProvider(event.includeServer(), new ExpLootProvider(output, registries));
    }
}
