package info.nightscout.androidaps.plugins.PumpDanaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.plugins.Overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.Overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.Overview.notifications.Notification;
import info.nightscout.androidaps.plugins.PumpDanaR.DanaRPump;
import info.nightscout.androidaps.plugins.PumpDanaRKorean.DanaRKoreanPlugin;

/**
 * Created by mike on 13.12.2016.
 */

public class MsgSettingMeal extends MessageBase {
    private static Logger log = LoggerFactory.getLogger(MsgSettingMeal.class);

    public MsgSettingMeal() {
        SetCommand(0x3203);
    }

    public void handleMessage(byte[] bytes) {
        DanaRPump pump = DanaRPump.getInstance();
        pump.basalStep = intFromBuff(bytes, 0, 1) / 100d;
        pump.bolusStep = intFromBuff(bytes, 1, 1) / 100d;
        boolean bolusEnabled = intFromBuff(bytes, 2, 1) == 1;
        int melodyTime = intFromBuff(bytes, 3, 1);
        int blockTime = intFromBuff(bytes, 4, 1);
        pump.isConfigUD = intFromBuff(bytes, 5, 1) == 1;

        if (Config.logDanaMessageDetail) {
            log.debug("Basal step: " + pump.basalStep);
            log.debug("Bolus step: " + pump.bolusStep);
            log.debug("Bolus enabled: " + bolusEnabled);
            log.debug("Melody time: " + melodyTime);
            log.debug("Block time: " + blockTime);
            log.debug("Is Config U/d: " + pump.isConfigUD);
        }

        // DanaRKorean is not possible to set to 0.01 but it works when controlled from AAPS
        if (DanaRKoreanPlugin.getPlugin().isEnabled(PluginBase.PUMP)) {
            pump.basalStep = 0.01d;
        }

        if (pump.basalStep != 0.01d) {
            Notification notification = new Notification(Notification.WRONGBASALSTEP, MainApp.sResources.getString(R.string.danar_setbasalstep001), Notification.URGENT);
            MainApp.bus().post(new EventNewNotification(notification));
        } else {
            MainApp.bus().post(new EventDismissNotification(Notification.WRONGBASALSTEP));
        }

        if (pump.isConfigUD) {
            Notification notification = new Notification(Notification.UD_MODE_ENABLED, MainApp.sResources.getString(R.string.danar_switchtouhmode), Notification.URGENT);
            MainApp.bus().post(new EventNewNotification(notification));
        } else {
            MainApp.bus().post(new EventDismissNotification(Notification.UD_MODE_ENABLED));
        }
    }

}
