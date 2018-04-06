package info.nightscout.androidaps.plugins.Actions;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.squareup.otto.Subscribe;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.db.ExtendedBolus;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventExtendedBolusChange;
import info.nightscout.androidaps.events.EventInitializationChanged;
import info.nightscout.androidaps.events.EventRefreshOverview;
import info.nightscout.androidaps.events.EventTempBasalChange;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.plugins.Actions.dialogs.FillDialog;
import info.nightscout.androidaps.plugins.Actions.dialogs.NewExtendedBolusDialog;
import info.nightscout.androidaps.plugins.Actions.dialogs.NewTempBasalDialog;
import info.nightscout.androidaps.plugins.Careportal.CareportalFragment;
import info.nightscout.androidaps.plugins.Careportal.Dialogs.NewNSTreatmentDialog;
import info.nightscout.androidaps.plugins.Careportal.OptionsToShow;
import info.nightscout.androidaps.plugins.Common.SubscriberFragment;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.utils.SingleClickButton;

/**
 * A simple {@link Fragment} subclass.
 */
public class ActionsFragment extends SubscriberFragment implements View.OnClickListener {

    static ActionsPlugin actionsPlugin = new ActionsPlugin();

    static public ActionsPlugin getPlugin() {
        return actionsPlugin;
    }

    SingleClickButton profileSwitch;
    SingleClickButton tempTarget;
    SingleClickButton extendedBolus;
    SingleClickButton extendedBolusCancel;
    SingleClickButton tempBasal;
    SingleClickButton tempBasalCancel;
    SingleClickButton fill;

    public ActionsFragment() {
        super();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.actions_fragment, container, false);

            profileSwitch = (SingleClickButton) view.findViewById(R.id.actions_profileswitch);
            tempTarget = (SingleClickButton) view.findViewById(R.id.actions_temptarget);
            extendedBolus = (SingleClickButton) view.findViewById(R.id.actions_extendedbolus);
            extendedBolusCancel = (SingleClickButton) view.findViewById(R.id.actions_extendedbolus_cancel);
            tempBasal = (SingleClickButton) view.findViewById(R.id.actions_settempbasal);
            tempBasalCancel = (SingleClickButton) view.findViewById(R.id.actions_canceltempbasal);
            fill = (SingleClickButton) view.findViewById(R.id.actions_fill);

            profileSwitch.setOnClickListener(this);
            tempTarget.setOnClickListener(this);
            extendedBolus.setOnClickListener(this);
            extendedBolusCancel.setOnClickListener(this);
            tempBasal.setOnClickListener(this);
            tempBasalCancel.setOnClickListener(this);
            fill.setOnClickListener(this);

            updateGUI();
            return view;
        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        return null;
    }

    @Subscribe
    public void onStatusEvent(final EventInitializationChanged ev) {
        updateGUI();
    }

    @Subscribe
    public void onStatusEvent(final EventRefreshOverview ev) {
        updateGUI();
    }

    @Subscribe
    public void onStatusEvent(final EventExtendedBolusChange ev) {
        updateGUI();
    }

    @Subscribe
    public void onStatusEvent(final EventTempBasalChange ev) {
        updateGUI();
    }

    @Override
    protected void updateGUI() {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (MainApp.getConfigBuilder().getActiveProfileInterface().getProfile() == null) {
                        tempTarget.setVisibility(View.GONE);
                        profileSwitch.setVisibility(View.GONE);
                        extendedBolus.setVisibility(View.GONE);
                        extendedBolusCancel.setVisibility(View.GONE);
                        tempBasal.setVisibility(View.GONE);
                        tempBasalCancel.setVisibility(View.GONE);
                        fill.setVisibility(View.GONE);
                        return;
                    }
                    final PumpInterface pump = ConfigBuilderPlugin.getActivePump();
                    if (!pump.getPumpDescription().isSetBasalProfileCapable || !pump.isInitialized() || pump.isSuspended())
                        profileSwitch.setVisibility(View.GONE);
                    else
                        profileSwitch.setVisibility(View.VISIBLE);


                    if (!pump.getPumpDescription().isExtendedBolusCapable || !pump.isInitialized() || pump.isSuspended() || pump.isFakingTempsByExtendedBoluses()) {
                        extendedBolus.setVisibility(View.GONE);
                        extendedBolusCancel.setVisibility(View.GONE);
                    } else {
                        if (MainApp.getConfigBuilder().isInHistoryExtendedBoluslInProgress()) {
                            extendedBolus.setVisibility(View.GONE);
                            extendedBolusCancel.setVisibility(View.VISIBLE);
                            ExtendedBolus running = MainApp.getConfigBuilder().getExtendedBolusFromHistory(System.currentTimeMillis());
                            extendedBolusCancel.setText(MainApp.instance().getString(R.string.cancel) + " " + running.toString());
                        } else {
                            extendedBolus.setVisibility(View.VISIBLE);
                            extendedBolusCancel.setVisibility(View.GONE);
                        }
                    }


                    if (!pump.getPumpDescription().isTempBasalCapable || !pump.isInitialized() || pump.isSuspended()) {
                        tempBasal.setVisibility(View.GONE);
                        tempBasalCancel.setVisibility(View.GONE);
                    } else {
                        if (MainApp.getConfigBuilder().isTempBasalInProgress()) {
                            tempBasal.setVisibility(View.GONE);
                            tempBasalCancel.setVisibility(View.VISIBLE);
                            final TemporaryBasal activeTemp = MainApp.getConfigBuilder().getTempBasalFromHistory(System.currentTimeMillis());
                            tempBasalCancel.setText(MainApp.instance().getString(R.string.cancel) + " " + activeTemp.toStringShort());
                        } else {
                            tempBasal.setVisibility(View.VISIBLE);
                            tempBasalCancel.setVisibility(View.GONE);
                        }
                    }

                    if (!pump.getPumpDescription().isRefillingCapable || !pump.isInitialized() || pump.isSuspended())
                        fill.setVisibility(View.GONE);
                    else
                        fill.setVisibility(View.VISIBLE);

                    if (!Config.APS)
                        tempTarget.setVisibility(View.GONE);
                    else
                        tempTarget.setVisibility(View.VISIBLE);
                }
            });
    }


    @Override
    public void onClick(View view) {
        FragmentManager manager = getFragmentManager();
        switch (view.getId()) {
            case R.id.actions_profileswitch:
                NewNSTreatmentDialog newDialog = new NewNSTreatmentDialog();
                final OptionsToShow profileswitch = CareportalFragment.PROFILESWITCH;
                profileswitch.executeProfileSwitch = true;
                newDialog.setOptions(profileswitch, R.string.careportal_profileswitch);
                newDialog.show(manager, "NewNSTreatmentDialog");
                break;
            case R.id.actions_temptarget:
                NewNSTreatmentDialog newTTDialog = new NewNSTreatmentDialog();
                final OptionsToShow temptarget = CareportalFragment.TEMPTARGET;
                temptarget.executeTempTarget = true;
                newTTDialog.setOptions(temptarget, R.string.careportal_temporarytarget);
                newTTDialog.show(manager, "NewNSTreatmentDialog");
                break;
            case R.id.actions_extendedbolus:
                NewExtendedBolusDialog newExtendedDialog = new NewExtendedBolusDialog();
                newExtendedDialog.show(manager, "NewExtendedDialog");
                break;
            case R.id.actions_extendedbolus_cancel:
                if (MainApp.getConfigBuilder().isInHistoryExtendedBoluslInProgress()) {
                    ConfigBuilderPlugin.getCommandQueue().cancelExtended(null);
                    Answers.getInstance().logCustom(new CustomEvent("CancelExtended"));
                }
                break;
            case R.id.actions_canceltempbasal:
                if (MainApp.getConfigBuilder().isTempBasalInProgress()) {
                    ConfigBuilderPlugin.getCommandQueue().cancelTempBasal(true, null);
                    Answers.getInstance().logCustom(new CustomEvent("CancelTemp"));
                }
                break;
            case R.id.actions_settempbasal:
                NewTempBasalDialog newTempDialog = new NewTempBasalDialog();
                newTempDialog.show(manager, "NewTempDialog");
                break;
            case R.id.actions_fill:
                FillDialog fillDialog = new FillDialog();
                fillDialog.show(manager, "FillDialog");
                break;
        }
    }
}
