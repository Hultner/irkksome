package se.alkohest.irkksome.model.impl

import android.content.ContentValues
import se.alkohest.irkksome.orm.BeanEntity
import se.alkohest.irkksome.orm.Transient

public class GradleSucks implements BeanEntity {
    @Transient
    private String transientString;

    @Override
    void setId(long id) {

    }

    @Override
    long getId() {
        return 0
    }

    @Override
    ContentValues createRow(long dependentPK) {
        return null
    }
}