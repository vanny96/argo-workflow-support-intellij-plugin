package io.vanny96.argoworkflowsupport

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.*

fun PsiElement.getYamlDocument(): YAMLDocument? {
    val file = containingFile
    if (file !is YAMLFile) return null

    val firstDocument = file.documents[0]
    if (firstDocument !is YAMLDocument) return null

    return firstDocument
}

fun YAMLValue.getYamlElementByKey(key: String): YAMLKeyValue? {
    if (this is YAMLMapping) {
        return getKeyValueByKey(key)
    }
    return null
}

fun YAMLDocument.getTemplates(): Map<String, ArgoTemplateRepresentation>? {
    val root = topLevelValue
    val kind = root?.getYamlElementByKey("kind")?.valueText

    return if (kind == "WorkflowTemplate") {
        val spec = root.getYamlElementByKey("spec")
        val templates = spec?.value?.getYamlElementByKey("templates")

        val templatesValue = templates?.value

        return if (templatesValue is YAMLSequence) {
            templatesValue.items.associate {
                val value = it.value as YAMLMapping
                val argoTemplate = ArgoTemplateRepresentation.fromYaml(value)
                argoTemplate.name.value to argoTemplate
            }
        } else {
            mapOf()
        }
    } else {
        null
    }
}

data class ArgoTemplateRepresentation(val name: ValueYamlPair<String>, val inputs: Map<String, ArgoParameterRepresentation>, val steps: List<ArgoStepRepresentation>) {

    companion object {
        fun fromYaml(yaml: YAMLMapping): ArgoTemplateRepresentation {
            val name = yaml.getYamlElementByKey("name")?.value!!
            val parameters = getParameters(yaml)
            val steps = getSteps(yaml)

            return ArgoTemplateRepresentation(ValueYamlPair(name, name.text), parameters, steps)
        }

        private fun getParameters(yaml: YAMLMapping): Map<String, ArgoParameterRepresentation> {
            val inputs = yaml.getYamlElementByKey("inputs")?.value
            val parameters = inputs?.getYamlElementByKey("parameters")?.value

            return if (parameters is YAMLSequence) {
                parameters.items
                        .mapNotNull { it.value }
                        .mapNotNull { ArgoParameterRepresentation.fromYaml(it) }
                        .associateBy { it.name.value }
            } else {
                mapOf()
            }
        }

        private fun getSteps(yaml: YAMLMapping): List<ArgoStepRepresentation> {
            val steps = yaml.getYamlElementByKey("steps")?.value

            fun getStepsFromSequence(sequence: YAMLSequence) = sequence.items.mapNotNull { it.value }
            val stepsSequence = if (steps is YAMLSequence) {
                getStepsFromSequence(steps)
                        .flatMap { if(it is YAMLSequence) getStepsFromSequence(it) else listOf(it) }
                        .map {ArgoStepRepresentation.fromYaml(it) }
            } else {
                listOf()
            }

            return stepsSequence
        }
    }
}

data class ArgoStepRepresentation(val name: ValueYamlPair<String>?, val template: ValueYamlPair<String>?, val parameters: List<ArgoParameterRepresentation>) {
    companion object {
        fun fromYaml(yaml: YAMLValue): ArgoStepRepresentation {
            val name = yaml.getYamlElementByKey("name")?.value
            val template = yaml.getYamlElementByKey("template")?.value

            val arguments = yaml.getYamlElementByKey("arguments")?.value
            val parameters = arguments?.getYamlElementByKey("parameters")?.value
            val parametersList = if (parameters is YAMLSequence) {
                parameters.items.mapNotNull { it.value }
                        .mapNotNull { ArgoParameterRepresentation.fromYaml(it) }
                        .toList()
            } else {
                listOf()
            }

            return ArgoStepRepresentation(
                    name = name?.let { ValueYamlPair(name, name.text) },
                    template = template?.let { ValueYamlPair(template, template.text) },
                    parameters = parametersList
            )
        }
    }
}

data class ArgoParameterRepresentation(val name: ValueYamlPair<String>, val value: ValueYamlPair<String>?) {
    companion object {
        fun fromYaml(yaml: YAMLValue): ArgoParameterRepresentation? {
            val name = yaml.getYamlElementByKey("name")?.value
            val value = yaml.getYamlElementByKey("value")?.value

            return name?.let {
                ArgoParameterRepresentation(
                        name = ValueYamlPair(name, name.text),
                        value = value?.let { ValueYamlPair(value, value.text) }
                )
            }
        }
    }
}

data class ValueYamlPair<T>(val yaml: YAMLValue, val value: T)