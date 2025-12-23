package lu.kolja.expandedae;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Expandedae.MODID)
public class ExpConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_CONTROLLER_SIZE = BUILDER
            .comment("The maximum allowed size of an ME controller.",
                    "Even tho it is minimal, as you increase this value, the controller will take longer to form.",
                    "Note: Controllers surrounded by more than 3 others in one axis, will remain gray but do still function normally.")
            .defineInRange("maxControllerSize", 7, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue IGNORE_CONTROLLER_RULES = BUILDER
            .comment("Ignore controller forming rules.",
                    "Allows you to make any shape you wish without the controller turning red.",
                    "Default value: false")
            .define("ignoreControllerRules", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxControllerSize;
    public static boolean ignoreControllerRules;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        maxControllerSize = MAX_CONTROLLER_SIZE.get();
        ignoreControllerRules = IGNORE_CONTROLLER_RULES.get();
    }
}
