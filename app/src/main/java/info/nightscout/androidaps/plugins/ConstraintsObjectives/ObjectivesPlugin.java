package info.nightscout.androidaps.plugins.ConstraintsObjectives;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import info.nightscout.androidaps.BuildConfig;
import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.interfaces.APSInterface;
import info.nightscout.androidaps.interfaces.ConstraintsInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.ConstraintsSafety.SafetyPlugin;
import info.nightscout.androidaps.plugins.Loop.LoopPlugin;
import info.nightscout.androidaps.plugins.NSClientInternal.NSClientInternalPlugin;
import info.nightscout.androidaps.plugins.PumpVirtual.VirtualPumpPlugin;
import info.nightscout.utils.SP;

/**
 * Created by mike on 05.08.2016.
 */
public class ObjectivesPlugin implements PluginBase, ConstraintsInterface {
    private static Logger log = LoggerFactory.getLogger(ObjectivesPlugin.class);

    private static ObjectivesPlugin objectivesPlugin;

    public static ObjectivesPlugin getPlugin() {
        if (objectivesPlugin == null) {
            objectivesPlugin = new ObjectivesPlugin();
        }
        return objectivesPlugin;
    }

    public static List<Objective> objectives;

    private boolean fragmentVisible = true;

    private ObjectivesPlugin() {
        initializeData();
        loadProgress();
        MainApp.bus().register(this);
    }

    @Override
    public String getFragmentClass() {
        return ObjectivesFragment.class.getName();
    }

    @Override
    public int getType() {
        return PluginBase.CONSTRAINTS;
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.objectives);
    }

    @Override
    public String getNameShort() {
        String name = MainApp.sResources.getString(R.string.objectives_shortname);
        if (!name.trim().isEmpty()) {
            //only if translation exists
            return name;
        }
        // use long name as fallback
        return getName();
    }

    @Override
    public boolean isEnabled(int type) {
        return type == CONSTRAINTS && ConfigBuilderPlugin.getActivePump().getPumpDescription().isTempBasalCapable;
    }

    @Override
    public boolean isVisibleInTabs(int type) {
        return type == CONSTRAINTS && fragmentVisible && !Config.NSCLIENT && !Config.G5UPLOADER;
    }

    @Override
    public boolean canBeHidden(int type) {
        return true;
    }

    @Override
    public boolean hasFragment() {
        return true;
    }

    @Override
    public boolean showInList(int type) {
        return true;
    }

    @Override
    public void setFragmentEnabled(int type, boolean fragmentEnabled) {
    }

    @Override
    public void setFragmentVisible(int type, boolean fragmentVisible) {
        if (type == CONSTRAINTS) this.fragmentVisible = fragmentVisible;
    }

    @Override
    public int getPreferencesId() {
        return -1;
    }

    class Objective {
        Integer num;
        String objective;
        String gate;
        Date started;
        Integer durationInDays;
        Date accomplished;

        Objective(Integer num, String objective, String gate, Date started, Integer durationInDays, Date accomplished) {
            this.num = num;
            this.objective = objective;
            this.gate = gate;
            this.started = started;
            this.durationInDays = durationInDays;
            this.accomplished = accomplished;
        }
    }

    // Objective 0
    public static boolean bgIsAvailableInNS = false;
    public static boolean pumpStatusIsAvailableInNS = false;
    // Objective 1
    public static Integer manualEnacts = 0;
    private static final Integer manualEnactsNeeded = 20;

    class RequirementResult {
        boolean done = false;
        String comment = "";

        RequirementResult(boolean done, String comment) {
            this.done = done;
            this.comment = comment;
        }
    }

    private String yesOrNo(boolean yes) {
        if (yes) return "☺";
        else return "---";
    }

    RequirementResult requirementsMet(Integer objNum) {
        switch (objNum) {
            case 0:
                boolean isVirtualPump = VirtualPumpPlugin.getPlugin().isEnabled(PluginBase.PUMP);
                boolean vpUploadEnabled = SP.getBoolean("virtualpump_uploadstatus", false);
                boolean vpUploadNeeded = !isVirtualPump || vpUploadEnabled;
                boolean hasBGData = DatabaseHelper.lastBg() != null;

                boolean apsEnabled = false;
                APSInterface usedAPS = ConfigBuilderPlugin.getActiveAPS();
                if (usedAPS != null && ((PluginBase) usedAPS).isEnabled(PluginBase.APS))
                    apsEnabled = true;

                return new RequirementResult(hasBGData && bgIsAvailableInNS && pumpStatusIsAvailableInNS && NSClientInternalPlugin.getPlugin().hasWritePermission() && LoopPlugin.getPlugin().isEnabled(PluginBase.LOOP) && apsEnabled && vpUploadNeeded,
                        MainApp.sResources.getString(R.string.objectives_bgavailableinns) + ": " + yesOrNo(bgIsAvailableInNS)
                                + "\n" + MainApp.sResources.getString(R.string.nsclienthaswritepermission) + ": " + yesOrNo(NSClientInternalPlugin.getPlugin().hasWritePermission())
                                + (isVirtualPump ? "\n" + MainApp.sResources.getString(R.string.virtualpump_uploadstatus_title) + ": " + yesOrNo(vpUploadEnabled) : "")
                                + "\n" + MainApp.sResources.getString(R.string.objectives_pumpstatusavailableinns) + ": " + yesOrNo(pumpStatusIsAvailableInNS)
                                + "\n" + MainApp.sResources.getString(R.string.hasbgdata) + ": " + yesOrNo(hasBGData)
                                + "\n" + MainApp.sResources.getString(R.string.loopenabled) + ": " + yesOrNo(LoopPlugin.getPlugin().isEnabled(PluginBase.LOOP))
                                + "\n" + MainApp.sResources.getString(R.string.apsselected) + ": " + yesOrNo(apsEnabled)
                );
            case 1:
                return new RequirementResult(manualEnacts >= manualEnactsNeeded,
                        MainApp.sResources.getString(R.string.objectives_manualenacts) + ": " + manualEnacts + "/" + manualEnactsNeeded);
            case 2:
                return new RequirementResult(true, "");
            case 3:
                boolean closedModeEnabled = SafetyPlugin.getPlugin().isClosedModeEnabled();
                return new RequirementResult(closedModeEnabled, MainApp.sResources.getString(R.string.closedmodeenabled) + ": " + yesOrNo(closedModeEnabled));
            case 4:
                double maxIOB = MainApp.getConfigBuilder().applyMaxIOBConstraints(1000d);
                boolean maxIobSet = maxIOB > 0;
                return new RequirementResult(maxIobSet, MainApp.sResources.getString(R.string.maxiobset) + ": " + yesOrNo(maxIobSet));
            default:
                return new RequirementResult(true, "");
        }
    }


    void initializeData() {
        bgIsAvailableInNS = false;
        pumpStatusIsAvailableInNS = false;
        manualEnacts = 0;

        objectives = new ArrayList<>();
        objectives.add(new Objective(0,
                MainApp.sResources.getString(R.string.objectives_0_objective),
                MainApp.sResources.getString(R.string.objectives_0_gate),
                new Date(0),
                0, // 0 day
                new Date(0)));
        objectives.add(new Objective(1,
                MainApp.sResources.getString(R.string.objectives_1_objective),
                MainApp.sResources.getString(R.string.objectives_1_gate),
                new Date(0),
                7, // 7 days
                new Date(0)));
        objectives.add(new Objective(2,
                MainApp.sResources.getString(R.string.objectives_2_objective),
                MainApp.sResources.getString(R.string.objectives_2_gate),
                new Date(0),
                0, // 0 days
                new Date(0)));
        objectives.add(new Objective(3,
                MainApp.sResources.getString(R.string.objectives_3_objective),
                MainApp.sResources.getString(R.string.objectives_3_gate),
                new Date(0),
                5, // 5 days
                new Date(0)));
        objectives.add(new Objective(4,
                MainApp.sResources.getString(R.string.objectives_4_objective),
                MainApp.sResources.getString(R.string.objectives_4_gate),
                new Date(0),
                1,
                new Date(0)));
        objectives.add(new Objective(5,
                MainApp.sResources.getString(R.string.objectives_5_objective),
                MainApp.sResources.getString(R.string.objectives_5_gate),
                new Date(0),
                7,
                new Date(0)));
        objectives.add(new Objective(6,
                MainApp.sResources.getString(R.string.objectives_6_objective),
                "",
                new Date(0),
                28,
                new Date(0)));
        objectives.add(new Objective(7,
                MainApp.sResources.getString(R.string.objectives_7_objective),
                "",
                new Date(0),
                28,
                new Date(0)));
    }

    public static void saveProgress() {
        if (objectives != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            for (int num = 0; num < objectives.size(); num++) {
                Objective o = objectives.get(num);
                editor.putString("Objectives" + num + "started", Long.toString(o.started.getTime()));
                editor.putString("Objectives" + num + "accomplished", Long.toString(o.accomplished.getTime()));
            }
            editor.putBoolean("Objectives" + "bgIsAvailableInNS", bgIsAvailableInNS);
            editor.putBoolean("Objectives" + "pumpStatusIsAvailableInNS", pumpStatusIsAvailableInNS);
            editor.putString("Objectives" + "manualEnacts", Integer.toString(manualEnacts));
            editor.apply();
            if (Config.logPrefsChange)
                log.debug("Objectives stored");
        }
    }

    private void loadProgress() {
        for (int num = 0; num < objectives.size(); num++) {
            Objective o = objectives.get(num);
            try {
                o.started = new Date(SP.getLong("Objectives" + num + "started", 0L));
                o.accomplished = new Date(SP.getLong("Objectives" + num + "accomplished", 0L));
            } catch (Exception e) {
                log.error("Unhandled exception", e);
            }
        }
        bgIsAvailableInNS = SP.getBoolean("Objectives" + "bgIsAvailableInNS", false);
        pumpStatusIsAvailableInNS = SP.getBoolean("Objectives" + "pumpStatusIsAvailableInNS", false);
        try {
            manualEnacts = SP.getInt("Objectives" + "manualEnacts", 0);
        } catch (Exception e) {
            log.error("Unhandled exception", e);
        }
        if (Config.logPrefsChange)
            log.debug("Objectives loaded");
    }

    /**
     * Constraints interface
     **/
    @Override
    public boolean isLoopEnabled() {
        return objectives.get(0).started.getTime() > 0;
    }

    @Override
    public boolean isClosedModeEnabled() {
        return objectives.get(3).started.getTime() > 0;
    }

    @Override
    public boolean isAutosensModeEnabled() {
        return objectives.get(5).started.getTime() > 0;
    }

    @Override
    public boolean isAMAModeEnabled() {
        return objectives.get(6).started.getTime() > 0;
    }

    @Override
    public boolean isSMBModeEnabled() {
        return objectives.get(7).started.getTime() > 0;
    }

    @Override
    public Double applyMaxIOBConstraints(Double maxIob) {
        if (objectives.get(3).started.getTime() > 0 && objectives.get(3).accomplished.getTime() == 0) {
            if (Config.logConstraintsChanges)
                log.debug("Limiting maxIOB " + maxIob + " to " + 0 + "U");
            return 0d;
        } else {
            return maxIob;
        }
    }

    @Override
    public Double applyBasalConstraints(Double absoluteRate) {
        return absoluteRate;
    }

    @Override
    public Integer applyBasalConstraints(Integer percentRate) {
        return percentRate;
    }

    @Override
    public Double applyBolusConstraints(Double insulin) {
        return insulin;
    }

    @Override
    public Integer applyCarbsConstraints(Integer carbs) {
        return carbs;
    }


}
