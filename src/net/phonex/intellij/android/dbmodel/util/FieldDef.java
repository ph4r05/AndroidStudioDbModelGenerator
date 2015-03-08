package net.phonex.intellij.android.dbmodel.util;

import com.intellij.psi.PsiField;

/**
 * Created by dusanklinec on 08.03.15.
 */
public class FieldDef {
    public String name;
    public String initializer;
    public PsiField field;

    public FieldDef(String name, String initializer, PsiField field) {
        this.name = name;
        this.initializer = initializer;
        this.field = field;
    }

    @Override
    public String toString() {
        return "FieldDef{" +
                "name='" + name + '\'' +
                ", initializer='" + initializer + '\'' +
                ", field=" + field +
                '}';
    }
}
