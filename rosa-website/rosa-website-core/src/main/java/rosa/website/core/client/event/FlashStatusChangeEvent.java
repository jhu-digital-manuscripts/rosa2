package rosa.website.core.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class FlashStatusChangeEvent extends GwtEvent<FlashStatusChangeEventHandler> {
    public static final Type<FlashStatusChangeEventHandler> TYPE = new Type<>();

    private final boolean newStatus;

    public FlashStatusChangeEvent(boolean newStatus) {
        this.newStatus = newStatus;
    }

    public boolean status() {
        return newStatus;
    }

    @Override
    public Type<FlashStatusChangeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FlashStatusChangeEventHandler handler) {
        handler.onFlashStatusChange(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlashStatusChangeEvent that = (FlashStatusChangeEvent) o;

        return newStatus == that.newStatus;

    }

    @Override
    public int hashCode() {
        return (newStatus ? 1 : 0);
    }

    @Override
    public String toString() {
        return "FlashStatusChangeEvent{" +
                "newStatus=" + newStatus +
                '}';
    }
}
