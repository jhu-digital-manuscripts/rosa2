package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;

public class CSVDataPlace extends Place {
    private final String name;

    public CSVDataPlace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CSVDataPlace)) return false;

        CSVDataPlace that = (CSVDataPlace) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CSVDataPlace{" +
                "name='" + name + '\'' +
                '}';
    }
}
