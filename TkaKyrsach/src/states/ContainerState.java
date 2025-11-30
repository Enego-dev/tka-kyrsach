package states;

public enum ContainerState {
    IDLE,
    MOVING_TO_LOAD,
    LOADING,
    MOVING_TO_UNLOAD,
    UNLOADING,
    EMERGENCY_STOP
}
