package com.visualselect.app

enum class CropMode {
    /** Inner edges of both hands — hands excluded from the saved image. */
    BETWEEN_HANDS,

    /** Bounding box around both hands — hands included in the saved image. */
    INCLUDE_HANDS,
}
