package ai.timefold.solver.model.maps.service.client.impl;

public class SingleItemCache<T> {

    private String id;
    private T item;

    public boolean isInCache(String id) {
        return id.equals(this.id);
    }

    public void put(String id, T item) {
        this.id = id;
        this.item = item;
    }

    public T get() {
        return item;
    }

    public void delete() {
        id = null;
        item = null;
    }

}
