package io.vanny96.argoworkflowsupport

import com.intellij.navigation.DirectNavigationProvider
import com.intellij.psi.PsiElement

class ArgoWorkflowCodeNavigation : DirectNavigationProvider {

    override fun getNavigationElement(element: PsiElement): PsiElement? {
        val document = element.getYamlDocument()

        val templates = document?.getTemplates() ?: return null

        for (template in templates) {
            for (step in template.value.steps) {
                getNavigationElementInStep(element, step, templates)?.let {
                    return it
                }
            }
        }

        return null
    }

    private fun getNavigationElementInStep(
            element: PsiElement,
            step: ArgoStepRepresentation,
            templates: Map<String, ArgoTemplateRepresentation>
    ): PsiElement? {
        if (step.template == null) return null
        val referenceTemplate = templates[step.template.value] ?: return null

        if (step.template.yaml == element) {
            return referenceTemplate.name.yaml
        }

        for (parameter in step.parameters) {
            if(parameter.name.yaml != element) break

            val referenceInput = referenceTemplate.inputs[parameter.name.value]
            if (referenceInput != null) {
                return referenceInput.name.yaml
            }
        }
        return null
    }
}