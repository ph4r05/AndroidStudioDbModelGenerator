/*
 * Copyright (C) 2013 Michał Charmas (http://blog.charmas.pl)
 * Copyright (C) 2015 Dusan Klinec Ph4r05.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.phonex.intellij.android.dbmodel;

import com.google.common.base.CaseFormat;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import net.phonex.intellij.android.dbmodel.typeserializers.*;
import net.phonex.intellij.android.dbmodel.util.FieldDef;
import net.phonex.intellij.android.dbmodel.util.NewFieldRecord;
import net.phonex.intellij.android.dbmodel.util.PsiUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Quite a few changes here by Dallas Gutauckis [dallas@gutauckis.com]
 */
public class CodeGenerator {
    public static final String CREATOR_NAME = "CREATOR";

    private final String[] integerTypes = new String[] {
            "byte", "double", "float", "int", "long", "short", "boolean",
            "java.lang.Byte", "java.lang.Double", "java.lang.Float",
            "java.lang.Integer", "java.lang.Long", "java.lang.Boolean"};

    private final PsiClass mClass;
    private final List<PsiField> mFields;
    private final TypeSerializerFactory mTypeSerializerFactory;

    public CodeGenerator(PsiClass psiClass, List<PsiField> fields) {
        mClass = psiClass;
        mFields = fields;

        this.mTypeSerializerFactory = new ChainSerializerFactory(
                new BundleSerializerFactory(),
                new DateSerializerFactory(),
                new EnumerationSerializerFactory(),
                new ParcelableListSerializerFactory(),
                new PrimitiveTypeSerializerFactory(),
                new PrimitiveArraySerializerFactory(),
                new ListSerializerFactory(),
                new ParcelableSerializerFactory(),
                new SerializableSerializerFactory()
        );
    }

    private String generateStaticCreator(PsiClass psiClass) {
        StringBuilder sb = new StringBuilder("public static final android.os.Parcelable.Creator<");

        String className = psiClass.getName();

        sb.append(className).append("> CREATOR = new android.os.Parcelable.Creator<").append(className).append(">(){")
                .append("public ").append(className).append(" createFromParcel(android.os.Parcel source) {")
                .append("return new ").append(className).append("(source);}")
                .append("public ").append(className).append("[] newArray(int size) {")
                .append("return new ").append(className).append("[size];}")
                .append("};");
        return sb.toString();
    }

    private String generateConstructor(List<PsiField> fields, PsiClass psiClass) {
        String className = psiClass.getName();

        StringBuilder sb = new StringBuilder("private ");

        // Create the Parcelable-required constructor
        sb.append(className).append("(android.os.Parcel in) {");

        // Creates all of the deserialization methods for the given fields
        for (PsiField field : fields) {
            sb.append(getSerializerForType(field).readValue(field, "in"));
        }

        sb.append("}");
        return sb.toString();
    }

    private String generateWriteToParcel(List<PsiField> fields) {
        StringBuilder sb = new StringBuilder("@Override public void writeToParcel(android.os.Parcel dest, int flags) {");

        for (PsiField field : fields) {
            sb.append(getSerializerForType(field).writeValue(field, "dest", "flags"));
        }

        sb.append("}");

        return sb.toString();
    }

    private TypeSerializer getSerializerForType(PsiField field) {
        return mTypeSerializerFactory.getSerializer(field.getType());
    }

    private String generateDescribeContents() {
        return "@Override public int describeContents() { return 0; }";
    }

    public void generate() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());

        removeExistingParcelableImplementation(mClass);

        // Describe contents method
        PsiMethod describeContentsMethod = elementFactory.createMethodFromText(generateDescribeContents(), mClass);
        // Method for writing to the parcel
        PsiMethod writeToParcelMethod = elementFactory.createMethodFromText(generateWriteToParcel(mFields), mClass);

        // Default constructor if needed
        String defaultConstructorString = generateDefaultConstructor(mClass);
        PsiMethod defaultConstructor = null;

        if (defaultConstructorString != null) {
            defaultConstructor = elementFactory.createMethodFromText(defaultConstructorString, mClass);
        }

        // Constructor
        PsiMethod constructor = elementFactory.createMethodFromText(generateConstructor(mFields, mClass), mClass);
        // CREATOR
        PsiField creatorField = elementFactory.createFieldFromText(generateStaticCreator(mClass), mClass);

        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());

        // Shorten all class references
        styleManager.shortenClassReferences(addAsLast(describeContentsMethod));
        styleManager.shortenClassReferences(addAsLast(writeToParcelMethod));

        // Only adds if available
        if (defaultConstructor != null) {
            styleManager.shortenClassReferences(addAsLast(defaultConstructor));
        }

        styleManager.shortenClassReferences(addAsLast(constructor));
        styleManager.shortenClassReferences(addAsLast(creatorField));

        makeClassImplementParcelable(elementFactory);
    }

    public void generateFields() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());

        List<NewFieldRecord> newFields = new ArrayList<NewFieldRecord>();
        newFields.add(new NewFieldRecord("TABLE", mClass.getName(), "TODO: verify"));
        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            newFields.add(new NewFieldRecord(fieldName, field.getName()));
        }

        PsiElement prevField = null;
        for (NewFieldRecord newField : newFields) {
            // Find if field is already present in file.
            FieldDef prevFieldImpl = PsiUtils.findField(mClass, newField.name);
            if (prevFieldImpl == null){
                PsiField curField  = elementFactory.createFieldFromText(newField.decl, mClass);
                PsiElement element = prevField == null ? addAsLast(curField) : mClass.addAfter(curField, prevField);
                prevField = styleManager.shortenClassReferences(element);
                continue;
            } else {
                prevField = prevFieldImpl.field;
            }

            // Present, do we have exact match of the values?
            final String valueInitializer = "\"" + newField.value + "\"";
            if (newField.value.equals(prevFieldImpl.initializer) || valueInitializer.equals(prevFieldImpl.initializer)){
                continue;
            }

            // Present and value differs, add anyway, but under previous field.
            newField.addComment("TODO: verify");
            PsiField curField  = elementFactory.createFieldFromText(newField.decl, mClass);
            PsiElement element = prevField == null ? addAsLast(curField) : mClass.addAfter(curField, prevField);
            prevField = styleManager.shortenClassReferences(element);
        }
    }

    public void generateFullProjection() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());
        StringBuilder sb = new StringBuilder("public static final String[] FULL_PROJECTION = new String[] {\n");
        final int nmFields = mFields.size();
        int cnFields = 0;

        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            String comma = cnFields + 1 == nmFields ? "" : ",";

            sb.append(fieldName).append(comma);
            cnFields += 1;
        }
        sb.append("\n};\n");

        FieldDef prevField = PsiUtils.findField(mClass, "FULL_PROJECTION");
        PsiField projectionField = elementFactory.createFieldFromText(sb.toString(), mClass);
        PsiElement element       = prevField == null ? addAsLast(projectionField) : mClass.addAfter(projectionField, prevField.field);
        styleManager.shortenClassReferences(element);
    }

    public void generateCreateTable() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());

        final String className = mClass.getQualifiedName();
        StringBuilder sb = new StringBuilder("public static final String CREATE_TABLE = \"CREATE TABLE IF NOT EXISTS \"\n");
        sb.append("+ TABLE\n");
        sb.append("+ \" (\"\n");

        // Compute longest field name.
        int maxFieldLen = 0;
        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            final int fLen = fieldName.length();
            if (fLen > maxFieldLen){
                maxFieldLen = fLen;
            }
        }

        // Build CREATE TABLE.
        final int nmFields = mFields.size();
        int cnFields = 0;
        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            String comma = cnFields + 1 == nmFields ? "" : ",";
            String sqlType = getSqlType(field);
            cnFields += 1;

            if ("FIELD_ID".equalsIgnoreCase(fieldName)){
                sb.append("+ ").append(rightPad(fieldName, maxFieldLen+3)).append(" + \" INTEGER PRIMARY KEY AUTOINCREMENT").append(comma).append(" \"\n");
                continue;
            }

            sb.append("+ ").append(rightPad(fieldName, maxFieldLen+3)).append(" + \" ").append(sqlType).append(comma).append(" \"\n");
        }

        sb.append("+ \");\";\n");

        FieldDef prevField = PsiUtils.findField(mClass, "CREATE_TABLE");
        PsiField projectionField = elementFactory.createFieldFromText(sb.toString(), mClass);
        PsiElement element       = prevField == null ? addAsLast(projectionField) : mClass.addAfter(projectionField, prevField.field);
        styleManager.shortenClassReferences(element);
    }

    public void generateCreateFromCursor() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());

        StringBuilder sb = new StringBuilder("private final void createFromCursor(Cursor c){\n" +
                "        int colCount = c.getColumnCount();\n" +
                "        for(int i=0; i<colCount; i++){\n" +
                "            final String colname = c.getColumnName(i);");

        final int nmFields = mFields.size();
        int cnFields = 0;

        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            final boolean lastOne = cnFields + 1 == nmFields;
            if (cnFields > 0){
                sb.append(" else ");
            }

            String sqlDeserializer = getSqlDeserializer(field);

            sb.append("if (").append(fieldName).append(".equals(colname)){\n");
            sb.append("this.").append(field.getName()).append(" = ").append(sqlDeserializer).append(";\n");
            sb.append("}");
            cnFields += 1;
        }

        if (nmFields > 0){
            sb.append("else {\n" +
                    "Log.w(THIS_FILE, \"Unknown column name: \" + colname);\n" +
                    "}");
        }

        sb.append("}\n}\n");

        PsiMethod prevMethod = PsiUtils.findMethod(mClass, "createFromCursor", "android.database.Cursor");
        PsiMethod method     = elementFactory.createMethodFromText(sb.toString(), mClass);
        PsiElement element   = prevMethod == null ? addAsLast(method) : mClass.addAfter(method, prevMethod);
        styleManager.shortenClassReferences(element);
    }

    public void generateGetDbContentValues() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());

        StringBuilder sb = new StringBuilder("public ContentValues getDbContentValues() {\n" +
                "    ContentValues args = new ContentValues();");

        final int nmFields = mFields.size();
        int cnFields = 0;

        for (PsiField field : mFields) {
            String fieldName = getFieldName(field.getName());
            PsiType fieldType = field.getType();
            String cnType = fieldType.getCanonicalText();
            final boolean lastOne = cnFields + 1 == nmFields;
            final boolean primitiveType = isPrimitiveType(fieldType);
            if (!primitiveType){
                sb.append("if (this."+(field.getName())+" != null)\n");
                sb.append("    ");
            }

            final String sqlSerializer = getSqlSerializer(field);
            sb.append("args.put(" + fieldName + ", " + (sqlSerializer) + ");\n");

            cnFields += 1;
        }

        sb.append("return args;\n}");

        PsiMethod prevMethod = PsiUtils.findMethod(mClass, "getDbContentValues");
        PsiMethod method     = elementFactory.createMethodFromText(sb.toString(), mClass);
        PsiElement element   = prevMethod == null ? addAsLast(method) : mClass.addAfter(method, prevMethod);
        styleManager.shortenClassReferences(element);
    }

    private String rightPad(String name, int desiredSize){
        final int ln = name.length();
        if (desiredSize <= ln){
            return name;
        }

        int toAdd = desiredSize - ln;
        StringBuilder sb = new StringBuilder(name);
        for(int i=0; i<toAdd; i++){
            sb.append(" ");
        }

        return sb.toString();
    }

    private boolean isPrimitiveType(PsiType type){
        final String cnType = type.getCanonicalText();
        return "byte".equals(cnType)
                || "char".equals(cnType)
                || "double".equals(cnType)
                || "float".equals(cnType)
                || "int".equals(cnType)
                || "long".equals(cnType)
                || "short".equals(cnType)
                || "boolean".equals(cnType)
                || "void".equals(cnType);
    }

    private String getSqlType(PsiField field){
        String typeStr = field.getType().getCanonicalText();

        // Check for blob.
        if ("byte[]".equals(typeStr)) {
            return "BLOB";
        }

        // Check for INTEGER.
        for (String integerType : integerTypes) {
            if (integerType.equals(typeStr)){
                return "INTEGER DEFAULT 0";
            }
        }

        // Special case - date.
        if ("java.util.Date".equals(typeStr)){
            return "INTEGER DEFAULT 0";
        }

        // By default, return TEXT.
        return "TEXT";
    }

    private String getSqlSerializer(PsiField field){
        String typeStr = field.getType().getCanonicalText();
        if ("java.util.Date".equalsIgnoreCase(typeStr)) {
            return (field.getName()) + ".getTime()";
        } else if ("boolean".equals(typeStr) || "java.lang.Boolean".equalsIgnoreCase(typeStr)){
            return field.getName() + " ? 1 : 0";
        } else {
            return field.getName();
        }
    }

    private String getSqlDeserializer(PsiField field){
        String typeStr = field.getType().getCanonicalText();
        if ("byte".equals(typeStr)) {
            return "(byte) c.getInt(i)";
        } else if ("java.lang.Byte".equals(typeStr)){
            return "(Byte) c.getInt(i)";
        } else if ("short".equals(typeStr)){
            return "c.getShort(i)";
        } else if ("int".equals(typeStr) || "java.lang.Integer".equals(typeStr)){
            return "c.getInt(i)";
        } else if ("long".equals(typeStr) || "java.lang.Long".equals(typeStr)){
            return "c.getLong(i)";
        }  else if ("double".equals(typeStr) || "java.lang.Double".equals(typeStr)){
            return "c.getDouble(i)";
        } else if ("float".equals(typeStr) || "java.lang.Float".equals(typeStr)){
            return "c.getFloat(i)";
        } else if ("byte[]".equals(typeStr)){
            return "c.getBlob(i)";
        } else if ("boolean".equals(typeStr)){
            return "c.getInt(i) == 1";
        } else if ("java.lang.Boolean".equals(typeStr)) {
            return "(Boolean) (c.getInt(i) == 1)";
        } else if ("java.util.Date".equals(typeStr)){
            return "new Date(c.getLong(i))";
        } else {
            return "c.getString(i)";
        }
    }

    private String getFieldName(String varName){
        return "FIELD_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, varName);
    }

    private PsiElement addAsLast(PsiElement elem){
        return mClass.addBefore(elem, mClass.getLastChild());
    }

    /**
     * Strips the
     * @param psiClass
     */
    private void removeExistingParcelableImplementation(PsiClass psiClass) {
        PsiField[] allFields = psiClass.getAllFields();

        // Look for an existing CREATOR and remove it
        for (PsiField field : allFields) {
            if (field.getName().equals(CREATOR_NAME)) {
                // Creator already exists, need to remove/replace it
                field.delete();
            }
        }

        findAndRemoveMethod(psiClass, psiClass.getName(), "android.os.Parcel");
        findAndRemoveMethod(psiClass, "describeContents");
        findAndRemoveMethod(psiClass, "writeToParcel", "android.os.Parcel", "int");
    }

    private String generateDefaultConstructor(PsiClass clazz) {
        // Check for any constructors; if none exist, we'll make a default one
        if (clazz.getConstructors().length == 0) {
            // No constructors exist, make a default one for convenience
            StringBuilder sb = new StringBuilder();
            sb.append("public ").append(clazz.getName()).append("(){}").append('\n');
            return sb.toString();
        } else {
        return null;
        }
    }

    private void makeClassImplementParcelable(PsiElementFactory elementFactory) {
        final PsiClassType[] implementsListTypes = mClass.getImplementsListTypes();
        final String implementsType = "android.os.Parcelable";

        for (PsiClassType implementsListType : implementsListTypes) {
            PsiClass resolved = implementsListType.resolve();

            // Already implements Parcelable, no need to add it
            if (resolved != null && implementsType.equals(resolved.getQualifiedName())) {
                return;
            }
        }

        PsiJavaCodeReferenceElement implementsReference = elementFactory.createReferenceFromText(implementsType, mClass);
        PsiReferenceList implementsList = mClass.getImplementsList();

        if (implementsList != null) {
            implementsList.add(implementsReference);
        }
    }

    private static void findAndRemoveMethod(PsiClass clazz, String methodName, String... arguments) {
        // Maybe there's an easier way to do this with mClass.findMethodBySignature(), but I'm not an expert on Psi*
        PsiMethod[] methods = clazz.findMethodsByName(methodName, false);

        for (PsiMethod method : methods) {
            PsiParameterList parameterList = method.getParameterList();

            if (parameterList.getParametersCount() == arguments.length) {
                boolean shouldDelete = true;

                PsiParameter[] parameters = parameterList.getParameters();

                for (int i = 0; i < arguments.length; i++) {
                    if (!parameters[i].getType().getCanonicalText().equals(arguments[i])) {
                        shouldDelete = false;
                    }
                }

                if (shouldDelete) {
                    method.delete();
                }
            }
        }
    }

}
