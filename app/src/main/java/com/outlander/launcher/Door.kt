package com.outlander.launcher

enum class Door(val label: String) {
    FRONT_LEFT("AV. G"),
    FRONT_RIGHT("AV. D"),
    REAR_LEFT("AR. G"),
    REAR_RIGHT("AR. D"),
    HOOD("Capot"),
    TRUNK("Coffre");

    companion object {
        val FL = FRONT_LEFT
        val FR = FRONT_RIGHT
        val RL = REAR_LEFT
        val RR = REAR_RIGHT
        val BOOT = TRUNK
        val TAILGATE = TRUNK
    }
}
