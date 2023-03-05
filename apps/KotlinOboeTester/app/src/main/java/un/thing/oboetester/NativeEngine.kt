package un.thing.oboetester

object NativeEngine {
    val isMMapSupported: Boolean
        external get
    val isMMapExclusiveSupported: Boolean
        external get

    external fun setWorkaroundsEnabled(enabled: Boolean)
    external fun areWorkaroundsEnabled(): Boolean
}
