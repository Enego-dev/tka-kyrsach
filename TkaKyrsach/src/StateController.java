import sensors.ControlSensors;
import sensors.InformationSensors;
import states.ContainerState;
import states.TransporterMoveDirection;

import java.util.NoSuchElementException;

public class StateController {
    private static ControlSensors controlSensors;
    private static InformationSensors informationSensors;
    private static ContainerState containerState;
    private static int[] words = new int[] {0b101, 0b101, 0b011, 0b010};
    private static byte fillTimer = 15;    // таймер заполнения на 15 секунд
    private static byte emergencyTimer = 20;   // аварийный таймер операции на 20 секунд
    private static byte indexR = -1;

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

    private static void findLoad(){
        if (!canFinishOperation1() && !canFinishOperation2()){
            containerState = ContainerState.FIND_UNLOAD;
            return;
        }

        int operation;
        int operationIndex;

        if (canFinishOperation1()){
            operation = words[0];
            operationIndex = 0;

            indexR = findIndex(operation, operationIndex);
            containerState = ContainerState.MOVING_TO_LOAD;
            return;
        }
    }

    private static byte findIndex(int operation, int operationIndex){
        if ((operation & 1) != 0 && informationSensors.getDRi(0)){
            operation &= ~1;
            words[operationIndex] = operation;
            return 0;   // R1
        } else if ((operation & 1 << 1) != 0 && informationSensors.getDRi(1)) {
            operation &= ~(1 << 1);
            words[operationIndex] = operation;
            return 1;   // R2
        } else if ((operation & 1 << 2) != 0 && informationSensors.getDRi(2)) {
            operation &= ~(1 << 2);
            words[operationIndex] = operation;
            return 2;   // R3
        } else {
            IO.println("Ошибка в поиске точки для наполнения из резервуара");
            IO.println(operation);
            IO.println(operationIndex);
            return -1;
        }
    }

    private static void movingToLoad(){
        var activeDPi = informationSensors.getActiveDPi();
        if (activeDPi == -1)
            throw new NoSuchElementException("Ни один из DPi неактивен! Бред!");

        if (indexR > activeDPi){
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_RIGHT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi + 1, true);
            return;
        } else if (indexR < activeDPi) {
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_LEFT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi - 1, true);
        }
        if (indexR != activeDPi)
            return;

        controlSensors.moveTransporter(TransporterMoveDirection.IDLE);
        indexR = -1;
        containerState = ContainerState.LOADING;

        /*if (index > getActiveDPi()){
            moveTransporter(TransporterMoveDirection.MOVE_RIGHT);
        } else if (index < getActiveDPi()){
            moveTransporter(TransporterMoveDirection.MOVE_LEFT);
        }

        setActiveDPi(index);
        moveTransporter(TransporterMoveDirection.IDLE);
        containerState = ContainerState.LOADING;*/
    }

    private static void loading(){

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

    // region Проверки на доступность выполнение хотя бы одной операции
    public static boolean canChangeStartState() {
        int testOp1 = 0;
        testOp1 = setBit(testOp1, 0, informationSensors.getDRi(0));
        testOp1 = setBit(testOp1, 1, informationSensors.getDRi(1));
        testOp1 = setBit(testOp1, 2, informationSensors.getDRi(2));

        int testOp2 = 0;
        testOp2 = setBit(testOp2, 0, informationSensors.getDRi(0));
        testOp2 = setBit(testOp2, 1, informationSensors.getDRi(1));
        testOp2 = setBit(testOp2, 2, informationSensors.getDRi(2));

        boolean cannotOp1 = informationSensors.getDj(words[1]) && (words[0] & testOp1) != words[0];
        boolean cannotOp2 = informationSensors.getDj(words[3]) && (words[2] & testOp2) != words[2];

        return cannotOp1 || cannotOp2;
    }

    private static boolean canFinishOperation1(){
        return words[0] != 0 && !informationSensors.getDj(words[1]);
    }

    private static boolean canFinishOperation2(){
        return words[2] != 0 && !informationSensors.getDj(words[3]);
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
