package info.nightscout.androidaps.events;

/**
 * Created by mike on 29.05.2017.
 */

public class EventReloadTreatmentData extends Event {
    public Object next;

    public EventReloadTreatmentData(Object next) {
        this.next = next;
    }
}
