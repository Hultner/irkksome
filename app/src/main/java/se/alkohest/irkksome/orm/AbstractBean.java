package se.alkohest.irkksome.orm;

public abstract class AbstractBean implements BeanEntity {
    private long id = -1;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
