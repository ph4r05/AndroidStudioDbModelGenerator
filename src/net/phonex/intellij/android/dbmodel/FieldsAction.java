/*
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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import net.phonex.intellij.android.dbmodel.util.PsiUtils;

import java.util.List;

public class FieldsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = PsiUtils.getPsiClassFromContext(e);

        GenerateDialog dlg = new GenerateDialog(psiClass);
        dlg.show();

        if (dlg.isOK()) {
            generateParcelable(psiClass, dlg.getSelectedFields());
        }
    }

    private void generateParcelable(final PsiClass psiClass, final List<PsiField> fields) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new CodeGenerator(psiClass, fields).generateFields();
            }
        }.execute();
    }


    @Override
    public void update(AnActionEvent e) {
        PsiClass psiClass = PsiUtils.getPsiClassFromContext(e);
        e.getPresentation().setEnabled(psiClass != null && !psiClass.isEnum() && !psiClass.isInterface());
    }


}
