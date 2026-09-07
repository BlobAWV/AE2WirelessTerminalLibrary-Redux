package tfar.ae2wt;

import appeng.core.AEConfig;
import tfar.ae2wt.config.ModConfig;

public class WTConfig {

    public static double getPowerMultiplier(double range, boolean isOutOfRange) {
        double baseMultiplier;
        if (!isOutOfRange) {
            baseMultiplier = AEConfig.instance().wireless_getDrainRate(range);
        } else {
            baseMultiplier = AEConfig.instance().wireless_getDrainRate(528 * ModConfig.COMMON.energyConsumptionMultiplier.get());
        }
        return baseMultiplier;
    }

    public static double getChargeRate() {
        return ModConfig.COMMON.chargeRate.get();
    }
}