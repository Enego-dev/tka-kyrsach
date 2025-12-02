import sensors.ControlSensors;
import sensors.InformationSensors;
import states.AvailableOperation;
import states.ContainerState;
import states.TransporterMoveDirection;

public class StateController {
    private static ControlSensors controlSensors;
    private static InformationSensors informationSensors;
    private static ContainerState containerState;
    private static int[] words = new int[] {0b101, 0b101, 0b011, 0b010};
    private static byte fillTimer = 15;    // таймер заполнения на 15 секунд
    private static byte emergencyTimer = 20;   // аварийный таймер операции на 20 секунд

    private static AvailableOperation currentOperation = AvailableOperation.NONE;
    private static int operationCodeToPerform = -1;
    private static int operationIndexToPerform = -1;
    private static byte indexP = -1;

    static void main() {
        containerState = ContainerState.START;
        controlSensors = new ControlSensors();
        informationSensors = new InformationSensors();
        controlSensors.setInformationSensors(informationSensors);
        informationSensors.setControlSensors(controlSensors);



        while (true){
            switch (containerState){
                case START -> start();
                case FIND_LOAD -> findLoad();
                case MOVING_TO_LOAD -> movingToLoad();
                case LOADING -> loading();
                case FIND_UNLOAD -> findUnload();
                case MOVING_TO_UNLOAD -> movingToUnload();
                case UNLOADING -> unloading();
                case STOP -> stop();
                case EMERGENCY_STOP -> emergencyStop();
                case null, default -> throw new IllegalStateException("Неверное состояние автомата!");
            }
        }
    }

    private static void start(){
        containerState = canChangeStartState() ? ContainerState.FIND_LOAD : ContainerState.STOP;
    }

    /// Отвечает только за indexR
    private static void findLoad(){
        currentOperation = getAvailableOperation();

        switch (currentOperation){
            case OPERATION_1 -> {
                operationCodeToPerform = words[0];
                operationIndexToPerform = 0;
            }
            case OPERATION_2 -> {
                operationCodeToPerform = words[2];
                operationIndexToPerform = 2;
            }
            case NONE -> {
                containerState = ContainerState.FIND_UNLOAD;
            }
            case null, default -> throw new RuntimeException("Беда беда беда системе((");
        }

        if (isBit(operationCodeToPerform, 0)){
            indexP = 1;
        } else if (isBit(operationCodeToPerform, 1)) {
            indexP = 2;
        } else if (isBit(operationCodeToPerform, 2)) {
            indexP = 3;
        } else {
            throw new RuntimeException("Ну, найти индекс Ri не удалось(");
        }

        containerState = ContainerState.MOVING_TO_LOAD;
    }

    private static void movingToLoad(){
        var activeDPi = informationSensors.getActiveDPi();
        if (activeDPi == -1){
            throw new RuntimeException("activeDPi = -1!!!");
        }

        if (indexP > activeDPi){
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_RIGHT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi + 1, true);
        } else if (indexP < activeDPi) {
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_LEFT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi - 1, true);
        }

        if (indexP != activeDPi)
            return;

        controlSensors.moveTransporter(TransporterMoveDirection.IDLE);
        containerState = ContainerState.LOADING;
    }

    private static void loading(){
        if (informationSensors.getDRi(indexP - 1)){
            throw new RuntimeException("Ожидалось, что резервуар будет наполнен, а он пустой!");
        }

        controlSensors.openValve(indexP - 1);
        controlSensors.closeValve(indexP - 1);
        indexP = -1;
        containerState = ContainerState.FIND_LOAD;
    }

    private static void findUnload(){

    }

    private static void movingToUnload(){

    }

    private static void unloading(){

    }

    private static void stop(){

    }

    private static void emergencyStop(){

    }

    // region Всякие разные проверки и получения
    public static boolean canChangeStartState() {
        return getAvailableOperation() != AvailableOperation.NONE;
    }

    private static int canPerformOperation(){
        int testOp = 0;
        testOp = setBit(testOp, 0, informationSensors.getDRi(0));
        testOp = setBit(testOp, 1, informationSensors.getDRi(1));
        testOp = setBit(testOp, 2, informationSensors.getDRi(2));
        return testOp;
    }

    private static AvailableOperation getAvailableOperation(){
        // если есть какие-то неотработанные резервуары и
        // и если резервуары
        if (words[0] != 0 && (words[0] & canPerformOperation()) != words[0] && !informationSensors.getDj(words[1])){
            return AvailableOperation.OPERATION_1;
        } else if (words[2] != 0 && (words[2] & canPerformOperation()) != words[2] && !informationSensors.getDj(words[3])) {
            return AvailableOperation.OPERATION_2;
        } else {
            return AvailableOperation.NONE;
        }
    }
    // endregion






    private static boolean isBit(int word, int index){
        return (word & 1 << index) != 0;
    }
    private static int setBit(int word, int index, boolean value){
        word &= ~(1 << index);
        if (value)
            word |= 1 << index;
        return word;
    }
}
