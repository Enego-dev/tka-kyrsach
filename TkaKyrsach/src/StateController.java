import sensors.ControlSensors;
import sensors.InformationSensors;
import states.AvailableOperation;
import states.ContainerRotateDirection;
import states.ContainerState;
import states.TransporterMoveDirection;

public class StateController {
    private static ControlSensors controlSensors = null;
    private static InformationSensors informationSensors = null;
    private static ContainerState containerState = null;
    private static int[] words = new int[] {0b101, 0b101, 0b011, 0b010};
    private static AvailableOperation currentOperation = null;
    private static int indexPi = -1;
    private static int currentTankMask = -1;
    private static boolean iscContainerFull = false;

    static void main() {
        containerState = ContainerState.INITIALIZE;
        controlSensors = new ControlSensors();
        informationSensors = new InformationSensors();
        controlSensors.setInformationSensors(informationSensors);
        //informationSensors.setControlSensors(controlSensors);
        containerState = ContainerState.WAITING;


        while (true){
            if (informationSensors.getAT() || informationSensors.getDT() || controlSensors.getEmergencyStop()){
                containerState = ContainerState.EMERGENCY_STOP;
            }

            switch (containerState){
                case WAITING -> waiting();
                case HANDLE_TANKS -> handleTanks();
                case MOVE_TO_TANK -> moveToTank();
                case LOADING -> loading();
                case MOVE_TO_UNLOAD -> moveToUnload();
                case ROTATE -> rotate();
                case UNLOADING -> unloading();
                case END -> end();
                case EMERGENCY_STOP -> emergencyStop();
                case null, default -> throw new RuntimeException("Че-то с автоматом пошло не так(");
            }
        }
    }

    private static void waiting(){
        var operation = getAvailableOperation();
        if (operation == AvailableOperation.NONE){
            containerState = ContainerState.END;
            return;
        }

        currentOperation = operation;
        containerState = ContainerState.HANDLE_TANKS;
    }

    private static void handleTanks(){
        // скорее всего так надо
        indexPi = -1;

        switch (currentOperation){
            case OPERATION_1 -> currentTankMask = words[0];
            case OPERATION_2 -> currentTankMask = words[2];
        }

        for (int i = 0; i < 3; i++) {
            if (isBit(currentTankMask, i)){
                indexPi = i + 1;
                containerState = ContainerState.MOVE_TO_TANK;
                return;
            }
        }

        if (indexPi == -1)
            throw new RuntimeException("Все же indexRi не нашелся!");
    }

    private static void moveToTank(){
        var activeDPi = informationSensors.getActiveDPi();
        if (activeDPi == -1)
            throw new RuntimeException("Active DPi = -1!");

        moveContainer();

        if (activeDPi != indexPi)
            return;

        containerState = ContainerState.LOADING;
    }

    private static void loading(){
        if (informationSensors.getDVi(indexPi - 1))
            throw new RuntimeException("Вроде проверялось, что есть содержимое в резервуаре!");

        controlSensors.openValve(indexPi);
        IO.println("Загрузка прошла успешно!");
        controlSensors.closeValve(indexPi);

        // обязательно обновить состояние
        iscContainerFull = true;
        int index = currentOperation == AvailableOperation.OPERATION_1 ? 0 : 2;
        words[index] = setBit(words[index], indexPi - 1, false);

        if (getAvailableOperation() == currentOperation){
            containerState = ContainerState.HANDLE_TANKS;
        } else if (getAvailableOperation() != currentOperation) {
            containerState = ContainerState.MOVE_TO_UNLOAD;
        }
    }

    private static void moveToUnload(){
        indexPi = getDPiByDj(getDjByOperation(currentOperation));
        moveContainer();
        if (informationSensors.getActiveDPi() != indexPi)
            return;

        containerState = ContainerState.ROTATE;
    }

    private static void rotate(){
        if (iscContainerFull){
            var bunkerNumber = getDjByOperation(currentOperation);
            // ПРОВЕРКА ПО ЗАДАНИЮ: бункер не должен быть заполнен!
            if (informationSensors.getDj(bunkerNumber)) {
                // Бункер заполнен → авария или пропуск
                containerState = ContainerState.EMERGENCY_STOP;
                return;
            }

            if (0 <= bunkerNumber && bunkerNumber <= 3)
                controlSensors.rotateTransporter(ContainerRotateDirection.ROTATE_LEFT);
            else if (4 <= bunkerNumber && bunkerNumber <= 7)
                controlSensors.rotateTransporter(ContainerRotateDirection.ROTATE_RIGHT);

            containerState = ContainerState.UNLOADING;
        } else {
            controlSensors.rotateTransporter(ContainerRotateDirection.NORMAL);
            containerState = ContainerState.WAITING;
        }
    }

    private static void unloading(){
        while (iscContainerFull){
            IO.println("разгрузка");
            iscContainerFull = false;
        }
        IO.println("Разгрузка завершена!");
        var bunkerNumber = getDjByOperation(currentOperation);
        informationSensors.setDj(bunkerNumber, true);
        containerState = ContainerState.ROTATE;
    }

    private static void emergencyStop(){
        containerState = ContainerState.END;
    }

    private static void end(){
        IO.println("Завершение работы программы...");
        System.exit(0);
    }


    // region Методы - условия входа в разные состояния автомата
    private static int getRiMask(){
        var mask = 0;
        mask = setBit(mask, 0, informationSensors.getDRi(0));
        mask = setBit(mask, 1, informationSensors.getDRi(1));
        mask = setBit(mask, 2, informationSensors.getDRi(2));
        return mask;
    }

    private static AvailableOperation getAvailableOperation(){
        int bunker1 = words[1];
        int bunker2 = words[3];
        if (words[0] != 0 && !informationSensors.getDj(bunker1) && (words[0] & getRiMask()) == 0){
            return AvailableOperation.OPERATION_1;
        } else if (words[2] != 0 && !informationSensors.getDj(bunker2) && (words[2] & getRiMask()) == 0) {
            return AvailableOperation.OPERATION_2;
        } else {
            return AvailableOperation.NONE;
        }
    }

    private static int getDjByOperation(AvailableOperation currentOperation){
        int dj = currentOperation == AvailableOperation.OPERATION_1 ? words[1] : words[3];
        return dj;
    }
    private static int getDPiByDj(int dj){
        return switch (dj){
            case 0,7 -> 0;
            case 1,6 -> 1;
            case 2,5 -> 2;
            case 3,4 -> 3;
            default -> throw new RuntimeException("Невозможно получить позицию DPi от Dj");
        };
    }

    private static void moveContainer(){
        var activeDPi = informationSensors.getActiveDPi();
        if (activeDPi == -1)
            throw new RuntimeException("Active DPi = -1!");

        if (indexPi > activeDPi){
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_RIGHT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi + 1, true);
        } else if (indexPi < activeDPi) {
            controlSensors.moveTransporter(TransporterMoveDirection.MOVE_LEFT);
            informationSensors.setDPi(activeDPi, false);
            informationSensors.setDPi(activeDPi - 1, true);
        }
    }
    // endregion



    // region Методы для работы с битами
    private static boolean isBit(int word, int index){
        return (word & 1 << index) != 0;
    }

    private static int setBit(int word, int index, boolean value){
        word &= ~(1 << index);
        if (value)
            word |= 1 << index;
        return word;
    }
    // endregion
}
