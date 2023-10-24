package io.vanny96.argoworkflowsupport

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class ArgoWorkflowCodeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : YamlPsiElementVisitor() {

        override fun visitDocument(document: YAMLDocument) {
            super.visitDocument(document)
            document.getTemplates()?.let{inspectTemplates(it)}
        }

        private fun inspectTemplates(templatesMap: Map<String, ArgoTemplateRepresentation>) {
            for (template in templatesMap.values) {
                for (step in template.steps) {

                    if (step.template == null) {
                        return
                    }

                    val referenceTemplate = templatesMap[step.template.value]
                    if (referenceTemplate == null) {
                        holder.registerProblem(step.template.yaml, "Referenced template does not exist")
                        return
                    }

                    for (parameter in step.parameters) {
                        val referenceParameter = referenceTemplate.inputs[parameter.name.value]

                        if (referenceParameter == null) {
                            holder.registerProblem(parameter.name.yaml, "Referenced parameter is not defined in template")
                        }
                    }
                }
            }
        }
    }
}