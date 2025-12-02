package states;

public enum ContainerState {
    START,
    FIND_LOAD,
    MOVING_TO_LOAD,
    LOADING,
    FIND_UNLOAD,
    MOVING_TO_UNLOAD,
    UNLOADING,
    STOP,
    EMERGENCY_STOP
}
