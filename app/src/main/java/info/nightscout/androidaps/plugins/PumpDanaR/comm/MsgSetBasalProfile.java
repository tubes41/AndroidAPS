package info.nightscout.androidaps.plugins.PumpDanaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.Overview.notifications.Notification;
import info.nightscout.androidaps.plugins.Overview.events.EventNewNotification;

public class MsgSetBasalProfile extends MessageBase {
    private static Logger log = LoggerFactory.getLogger(MsgSetBasalProfile.class);

    public MsgSetBasalProfile() {
        SetCommand(0x3306);
    }

    // index 0-3
    public MsgSetBasalProfile(byte index, double[] values) {
        this();
        AddParamByte(index);
        for (Integer i = 0; i < 24; i++) {
            AddParamInt((int) (values[i] * 100));
        }
        if (Config.logDanaMessageDetail)
            log.debug("Set basal profile: " + index);
    }

    @Override
    public void handleMessage(byte[] bytes) {
        int result = intFromBuff(bytes, 0, 1);
        if (result != 1) {
            failed = true;
            log.debug("Set basal profile result: " + result + " FAILED!!!");
            Notification reportFail = new Notification(Notification.PROFILE_SET_FAILED, MainApp.sResources.getString(R.string.profile_set_failed), Notification.URGENT);
            MainApp.bus().post(new EventNewNotification(reportFail));
        } else {
            if (Config.logDanaMessageDetail)
                log.debug("Set basal profile result: " + result);
            Notification reportOK = new Notification(Notification.PROFILE_SET_OK, MainApp.sResources.getString(R.string.profile_set_ok), Notification.INFO, 60);
            MainApp.bus().post(new EventNewNotification(reportOK));
        }
    }


}
