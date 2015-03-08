package net.phonex.intellij.android.dbmodel.util;

/**
 * Created by dusanklinec on 08.03.15.
 */
public class NewFieldRecord {
    public String decl;
    public final String name;
    public final String value;
    public String comment;

    public NewFieldRecord(String name, String value) {
        this.name = name;
        this.value = value;
        setup();
    }

    public NewFieldRecord(String name, String value, String comment) {
        this.value = value;
        this.name = name;
        this.comment = comment;
        setup();
    }

    public void setup(){
        this.decl = "extern NSString * " + name + "; \n";
        if (comment == null) {
            this.decl = "public static final String "+name+" = \""+value+"\";\n";
        } else {
            this.decl = "public static final String "+name+" = \""+value+"\"; //" + comment + "\n";
        }
    }

    public void addComment(String s) {
        if (this.comment == null){
            this.comment = s;
        } else {
            this.comment = this.comment + "; " + s;
        }

        setup();
    }
}
