package com.vrikshaayush.model

data class DiseaseInfo(
    val id: String = "",
    val disease_name: String = "",
    val model_labels: List<String>? = null,
    val crop_type: String = "",
    val severity_default: String = "MEDIUM",
    val description: Map<String, String> = emptyMap(),
    val common_in: List<String>? = null,
    val symptoms: Map<String, List<String>> = emptyMap(),
    val causes: Map<String, List<String>> = emptyMap(),
    val prevention_tips: Map<String, List<String>> = emptyMap(),
    val organic_treatments: Map<String, List<Treatment>> = emptyMap(),
    val chemical_treatments: Map<String, List<Treatment>> = emptyMap()
)

data class Treatment(
    val step: Int = 0,
    val title: String = "",
    val description: String = ""
)
