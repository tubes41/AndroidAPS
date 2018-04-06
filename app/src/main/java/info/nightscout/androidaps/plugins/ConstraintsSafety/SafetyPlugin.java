package info.nightscout.androidaps.plugins.ConstraintsSafety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import info.nightscout.androidaps.BuildConfig;
import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.interfaces.ConstraintsInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.utils.HardLimits;
import info.nightscout.utils.Round;
import info.nightscout.utils.SP;

/**
 * Created by mike on 05.08.2016.
 */
public class SafetyPlugin implements PluginBase, ConstraintsInterface {
    private static Logger log = LoggerFactory.getLogger(SafetyPlugin.class);

    static SafetyPlugin plugin = null;

    public static SafetyPlugin getPlugin() {
        if (plugin == null)
            plugin = new SafetyPlugin();
        return plugin;
    }

    @Override
    public String getFragmentClass() {
        return null;
    }

    @Override
    public int getType() {
        return PluginBase.CONSTRAINTS;
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.safety);
    }

    @Override
    public String getNameShort() {
        // use long name as fallback (no tabs)
        return getName();
    }

    @Override
    public boolean isEnabled(int type) {
        return type == CONSTRAINTS;
    }

    @Override
    public boolean isVisibleInTabs(int type) {
        return false;
    }

    @Override
    public boolean canBeHidden(int type) {
        return true;
    }

    @Override
    public boolean hasFragment() {
        return false;
    }

    @Override
    public boolean showInList(int type) {
        return false;
    }

    @Override
    public void setFragmentEnabled(int type, boolean fragmentEnabled) {

    }

    @Override
    public void setFragmentVisible(int type, boolean fragmentVisible) {
    }

    @Override
    public int getPreferencesId() {
        return R.xml.pref_safety;
    }

    @Override
    public boolean isLoopEnabled() {
        return ConfigBuilderPlugin.getActivePump().getPumpDescription().isTempBasalCapable;
    }

    /**
     * Constraints interface
     **/
    @Override
    public boolean isClosedModeEnabled() {
        String mode = SP.getString("aps_mode", "open");
        return mode.equals("closed") && BuildConfig.CLOSEDLOOP;
    }

    @Override
    public boolean isAutosensModeEnabled() {
        return true;
    }

    @Override
    public boolean isAMAModeEnabled() {
        return true;
    }

    @Override
    public boolean isSMBModeEnabled() {
        return true;
    }

    @Override
    public Double applyBasalConstraints(Double absoluteRate) {
        Double origAbsoluteRate = absoluteRate;
        Double maxBasal = SP.getDouble("openapsma_max_basal", 1d);

        Profile profile = MainApp.getConfigBuilder().getProfile();
        if (profile == null) return absoluteRate;
        if (absoluteRate < 0) absoluteRate = 0d;

        Integer maxBasalMult = SP.getInt("openapsama_current_basal_safety_multiplier", 4);
        Integer maxBasalFromDaily = SP.getInt("openapsama_max_daily_safety_multiplier", 3);
        // Check percentRate but absolute rate too, because we know real current basal in pump
        Double origRate = absoluteRate;
        if (absoluteRate > maxBasal) {
            absoluteRate = maxBasal;
            if (Config.logConstraintsChanges && origAbsoluteRate != Constants.basalAbsoluteOnlyForCheckLimit)
                log.debug("Limiting rate " + origRate + " by maxBasal preference to " + absoluteRate + "U/h");
        }
        if (absoluteRate > maxBasalMult * profile.getBasal()) {
            absoluteRate = Math.floor(maxBasalMult * profile.getBasal() * 100) / 100;
            if (Config.logConstraintsChanges && origAbsoluteRate != Constants.basalAbsoluteOnlyForCheckLimit)
                log.debug("Limiting rate " + origRate + " by maxBasalMult to " + absoluteRate + "U/h");
        }
        if (absoluteRate > profile.getMaxDailyBasal() * maxBasalFromDaily) {
            absoluteRate = profile.getMaxDailyBasal() * maxBasalFromDaily;
            if (Config.logConstraintsChanges && origAbsoluteRate != Constants.basalAbsoluteOnlyForCheckLimit)
                log.debug("Limiting rate " + origRate + " by 3 * maxDailyBasal to " + absoluteRate + "U/h");
        }
        return absoluteRate;
    }

    @Override
    public Integer applyBasalConstraints(Integer percentRate) {
        Integer origPercentRate = percentRate;
        Double maxBasal = SP.getDouble("openapsma_max_basal", 1d);

        Profile profile = MainApp.getConfigBuilder().getProfile();
        if (profile == null) return percentRate;
        Double currentBasal = profile.getBasal();

        Double absoluteRate = currentBasal * ((double) percentRate / 100);

        if (Config.logConstraintsChanges)
            log.debug("Percent rate " + percentRate + "% recalculated to " + absoluteRate + "U/h with current basal " + currentBasal + "U/h");

        if (absoluteRate < 0) absoluteRate = 0d;

        Integer maxBasalMult = SP.getInt("openapsama_current_basal_safety_multiplier", 4);
        Integer maxBasalFromDaily = SP.getInt("openapsama_max_daily_safety_multiplier", 3);
        // Check percentRate but absolute rate too, because we know real current basal in pump
        Double origRate = absoluteRate;
        if (absoluteRate > maxBasal) {
            absoluteRate = maxBasal;
            if (Config.logConstraintsChanges && !Objects.equals(origPercentRate, Constants.basalPercentOnlyForCheckLimit))
                log.debug("Limiting rate " + origRate + " by maxBasal preference to " + absoluteRate + "U/h");
        }
        if (absoluteRate > maxBasalMult * profile.getBasal()) {
            absoluteRate = Math.floor(maxBasalMult * profile.getBasal() * 100) / 100;
            if (Config.logConstraintsChanges && !Objects.equals(origPercentRate, Constants.basalPercentOnlyForCheckLimit))
                log.debug("Limiting rate " + origRate + " by maxBasalMult to " + absoluteRate + "U/h");
        }
        if (absoluteRate > profile.getMaxDailyBasal() * maxBasalFromDaily) {
            absoluteRate = profile.getMaxDailyBasal() * maxBasalFromDaily;
            if (Config.logConstraintsChanges && !Objects.equals(origPercentRate, Constants.basalPercentOnlyForCheckLimit))
                log.debug("Limiting rate " + origRate + " by 3 * maxDailyBasal to " + absoluteRate + "U/h");
        }

        Integer percentRateAfterConst = new Double(absoluteRate / currentBasal * 100).intValue();
        if (percentRateAfterConst < 100)
            percentRateAfterConst = Round.ceilTo((double) percentRateAfterConst, 10d).intValue();
        else percentRateAfterConst = Round.floorTo((double) percentRateAfterConst, 10d).intValue();

        if (Config.logConstraintsChanges && !Objects.equals(origPercentRate, Constants.basalPercentOnlyForCheckLimit))
            log.debug("Recalculated percent rate " + percentRate + "% to " + percentRateAfterConst + "%");
        return percentRateAfterConst;
    }

    @Override
    public Double applyBolusConstraints(Double insulin) {
        try {
            Double maxBolus = SP.getDouble("treatmentssafety_maxbolus", 3d);

            if (insulin < 0) insulin = 0d;
            if (insulin > maxBolus) insulin = maxBolus;
        } catch (Exception e) {
            insulin = 0d;
        }
        if (insulin > HardLimits.maxBolus()) insulin = HardLimits.maxBolus();
        return insulin;
    }

    @Override
    public Integer applyCarbsConstraints(Integer carbs) {
        try {
            Integer maxCarbs = SP.getInt("treatmentssafety_maxcarbs", 48);

            if (carbs < 0) carbs = 0;
            if (carbs > maxCarbs) carbs = maxCarbs;
        } catch (Exception e) {
            carbs = 0;
        }
        return carbs;
    }

    @Override
    public Double applyMaxIOBConstraints(Double maxIob) {
        return maxIob;
    }

}
