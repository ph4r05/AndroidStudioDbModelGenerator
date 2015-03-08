package net.phonex.intellij.android.dbmodel.util;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for introspecting Psi* stuff
 *
 * @author Dallas Gutauckis [dallas@gutauckis.com]
 */
final public class PsiUtils {
    private PsiUtils() {
    }

    public static PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (psiFile == null || editor == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);

        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    /**
     * Checks that the given type is an implementer of the given canonicalName with the given typed parameters
     *
     * @param type                what we're checking against
     * @param canonicalName       the type must extend/implement this generic
     * @param canonicalParamNames the type that the generic(s) must be (in this order)
     * @return
     */
    public static boolean isTypedClass(PsiType type, String canonicalName, String... canonicalParamNames) {
        PsiClass parameterClass = PsiTypesUtil.getPsiClass(type);

        if (parameterClass == null) {
            return false;
        }

        // This is a safe cast, for if parameterClass != null, the type was checked in PsiTypesUtil#getPsiClass(...)
        PsiClassType pct = (PsiClassType) type;

        // Main class name doesn't match; exit early
        if (!canonicalName.equals(parameterClass.getQualifiedName())) {
            return false;
        }

        List<PsiType> psiTypes = new ArrayList<PsiType>(pct.resolveGenerics().getSubstitutor().getSubstitutionMap().values());

        for (int i = 0; i < canonicalParamNames.length; i++) {
            if (!isOfType(psiTypes.get(i), canonicalParamNames[i])) {
                return false;
            }
        }

        // Passed all screenings; must be a match!
        return true;
    }

    /**
     * Resolves generics on the given type and returns them (if any) or null if there are none
     *
     * @param type
     * @return
     */
    public static List<PsiType> getResolvedGenerics(PsiType type) {
        List<PsiType> psiTypes = null;

        if (type instanceof PsiClassType) {
            PsiClassType pct = (PsiClassType) type;
            psiTypes = new ArrayList<PsiType>(pct.resolveGenerics().getSubstitutor().getSubstitutionMap().values());
        }

        return psiTypes;
    }

    public static boolean isOfType(PsiType type, String canonicalName) {
        if (type.getCanonicalText().equals(canonicalName)) {
            return true;
        }

        if (getNonGenericType(type).equals(canonicalName)) {
            return true;
        }

        for (PsiType iterType : type.getSuperTypes()) {
            if (iterType.getCanonicalText().equals(canonicalName) || getNonGenericType(iterType).equals(canonicalName)) {
                return true;
            }
        }

        return false;
    }

    public static String getNonGenericType(PsiType type) {
        if (type instanceof PsiClassType) {
            PsiClassType pct = (PsiClassType) type;
            return pct.resolve().getQualifiedName();
        }

        return type.getCanonicalText();
    }

    public static PsiElement addLast(PsiElement elem, PsiElement where){
        return where.addBefore(elem, where.getLastChild());
    }

    public static PsiMethod findMethod(PsiClass cls, String methodName, String... arguments) {
        if (cls == null){
            return null;
        }

        // Maybe there's an easier way to do this with mClass.findMethodBySignature(), but I'm not an expert on Psi*
        PsiMethod[] methods = cls.findMethodsByName(methodName, false);
        for (PsiMethod method : methods) {
            PsiParameterList parameterList = method.getParameterList();

            if (parameterList.getParametersCount() == arguments.length) {
                boolean shouldReturn = true;

                PsiParameter[] parameters = parameterList.getParameters();

                for (int i = 0; i < arguments.length; i++) {
                    if (!parameters[i].getType().getCanonicalText().equals(arguments[i])) {
                        shouldReturn = false;
                    }
                }

                if (shouldReturn) {
                    return method;
                }
            }
        }

        return null;
    }

    public static FieldDef findField(PsiClass cls, String fieldName){
        if (cls == null){
            return null;
        }

        PsiField field = cls.findFieldByName(fieldName, true);
        if (field == null){
            return null;
        }

        String initializerString = null;
        final PsiExpression initializer = field.getInitializer();
        if (initializer instanceof PsiLiteralExpression){
            final PsiLiteralExpression itext = (PsiLiteralExpression) initializer;
            initializerString = itext.getText();
        }

        return new FieldDef(field.getName(), initializerString, field);
    }

}
