import kotlinx.serialization.Serializable

@Serializable
public data class CheckpointModelSpec (
    val checkpointWait: Long = 60*60*1000,
    val checkpointTime: Long = 5*60*1000
)
