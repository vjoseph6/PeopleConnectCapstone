package com.capstone.peopleconnect.Classes

data class Skills(
    val skillItems: List<SkillItem> = emptyList(),
    val user: String = ""
)

data class SkillItem(
    val name: String = "",
    var visible: Boolean = true,
    val image: String = "",
    var description: String = "",
    var skillRate: Int = 0
)
