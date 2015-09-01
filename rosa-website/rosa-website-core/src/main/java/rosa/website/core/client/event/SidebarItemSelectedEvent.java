package rosa.website.core.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class SidebarItemSelectedEvent extends GwtEvent<SidebarItemSelectedEventHandler> {
    public static final Type<SidebarItemSelectedEventHandler> TYPE = new Type<>();

    public final String selectedItem;

    public SidebarItemSelectedEvent(String selectedItem) {
        this.selectedItem = selectedItem;
    }

    @Override
    public Type<SidebarItemSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SidebarItemSelectedEventHandler handler) {
        handler.onSelected(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SidebarItemSelectedEvent that = (SidebarItemSelectedEvent) o;

        return !(selectedItem != null ? !selectedItem.equals(that.selectedItem) : that.selectedItem != null);

    }

    @Override
    public int hashCode() {
        return selectedItem != null ? selectedItem.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SidebarItemSelectedEvent{" +
                "selectedItem='" + selectedItem + '\'' +
                '}';
    }
}
