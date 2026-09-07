package tfar.ae2wt.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

public class ModConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.DoubleValue magnetRange;
        public final ForgeConfigSpec.IntValue magnetTickInterval;
        public final ForgeConfigSpec.DoubleValue energyConsumptionMultiplier;
        public final ForgeConfigSpec.DoubleValue chargeRate;

        CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Magnet Card settings").push("magnet");
            magnetRange = builder
                    .comment("Range in blocks for magnet pickup. Be aware that increasing this value can cause severe server lag.")
                    .defineInRange("range", 8, 2.0, Double.MAX_VALUE);
            magnetTickInterval = builder
                    .comment("Interval in ticks between magnet scans (1 = every tick). There are 20 ticks in 1 second. Be aware that decreasing this value can cause severe server lag.")
                    .defineInRange("tickInterval", 10, 1, Integer.MAX_VALUE );
            builder.pop();

            builder.comment("Energy settings").push("energy");
            energyConsumptionMultiplier = builder
                    .comment("Multiplier for wireless terminal energy consumption if the player is out of range of the network (528+ blocks). You can change base energy consumption in Applied Energistics 2 config.")
                    .defineInRange("consumptionMultiplier", 2.0, 1.0, Double.MAX_VALUE);
            chargeRate = builder
                    .comment("Charge rate for wireless terminals in AE/tick.")
                    .defineInRange("chargeRate", 32000.0, 1000.0, Double.MAX_VALUE);
            builder.pop();
        }
    }
}