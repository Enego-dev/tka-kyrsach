public class AutomaticMachineCore {
    // открыть (true) или закрыть (false) задвижку резервуаров
    private boolean[] Vi = new boolean[3];
    // транспортер вправо
    private boolean MR = false;
    // транспортер влево
    private boolean ML = false;
    // повернуть контейнер вдоль часовой стрелки (вправо) для сброса в B4..B7
    private boolean MKR = false;
    // повернуть контейнер против часовой стрелки (влево) для сброса в B0..B3
    private boolean MKL = false;
    // аварийная остановка
    private boolean EMERGENCY_STOP = false;



    // false - есть материал, true - нет материала
    private boolean[] DRi = new boolean[] {false, false, false};  // Сработал ли датчик пустоты в резервуаре Ri
    // false - закрыт, true - открыт
    private boolean[] DVi = new boolean[] {false, false, false};  // Открыт ли клапан резервуара Ri
    private boolean[] DPi = new boolean[4];   // Находится ли контейнер в позиции P0...P3
    private boolean DKL = false; // Поворачивается ли контейнер влево
    private boolean DKR = false; // Поворачивается ли контейнер вправо
    private boolean DN = true;   // В нейтральном ли положении контейнер
    // false - не заполнен, true - заполнен
    private boolean[] Dj = new boolean[8];   // Заполнен ли бункер Dj
    private boolean BK0 = false; // Вышел ли контейнер за заданный левый предел
    private boolean BK1 = false; // Вышел ли контейнер за заданный правый предел
    private boolean DT = false;  // Сработал ли таймер заполнения
    private boolean AT = false;  // Сработал ли аварийный сигнал от таймера превышения ожидаемого времени операции



    enum ContainerState {
        INITIALIZING,
        PROCESS_CONTROL_CODE,
        MOVING,
        LOADING,
        ROTATING,
        UNLOADING,
        END,
        COMPLETE_OPERATION,
        EMERGENCY_STOP
    }



    // region Методы конечного автомата
    private void openValve(int indexDP){
        if (indexDP < 0 || indexDP > 3){
            throw new IllegalArgumentException("indexDP out of range [0..3]");
        }

        var valveIndex = getValveIndex(indexDP);

        // находится ли контейнер под нужным резервуаром?
        if (!DPi[indexDP]){
            throw new IllegalArgumentException("Container is not under this tank!");
        }

        // резервуар пустой?
        if (DRi[valveIndex]){
            throw new IllegalArgumentException("Tank in this position is empty!");
        }

        // у него открыт клапан?
        if (DVi[valveIndex]){
            throw new IllegalArgumentException("Valve in this position is opened!");
        }

        if (!DN){
            throw new IllegalArgumentException("Container should be in default rotation!");
        }

        Vi[valveIndex] = DVi[valveIndex] = true;
    }

    private void closeValve(int indexDP){
        if (indexDP < 0 || indexDP > 3){
            throw new IllegalArgumentException("indexDP out of range [0..3]");
        }

        var valveIndex = getValveIndex(indexDP);

        // находится ли контейнер под нужным резервуаром?
        if (!DPi[valveIndex]){
            throw new IllegalArgumentException("Container is not under this tank!");
        }

        // у него закрыт клапан?
        if (!DVi[valveIndex]){
            return;
        }

        DRi[valveIndex] = DVi[valveIndex] = false;
    }

    private int getValveIndex(int indexDP) {
        return switch (indexDP){
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            default -> throw new IllegalArgumentException("indexDP out of range [1..3]");
        };
    }

    private void moveTransporter(int indexDP){
        if (indexDP < 0 || indexDP > 3){
            throw new IllegalArgumentException("indexDP out of range [0..3]");
        }

        // может быть, транспортер уже под DPi?
        if (DPi[indexDP]){
            MR = ML = false;
            return;
        }

        // получим индекс DP, где сейчас контейнер
        int activeDPi = -1;
        for (int i = 0; i < DPi.length; i++) {
            if (DPi[i]){
                activeDPi = i;
                break;  // причем он только один из 4
            }
        }

        while (!DPi[indexDP]){
            if (indexDP > activeDPi){
                activeDPi += 1;
                setDPi(activeDPi);
                MR = true;
                ML = false;
            } else if (indexDP < activeDPi){
                activeDPi -= 1;
                setDPi(activeDPi);
                ML = true;
                MR = false;
            }
        }
    }

    private void setDPi(int indexDP){
        if (indexDP < 0 || indexDP > 3){
            throw new IllegalArgumentException("indexDP out of range [0..3]");
        }

        DPi[0] = DPi[1] = DPi[2] = false;
        DPi[indexDP] = true;
    }

    private void rotateContainerToBunker(int indexDj){
        if (indexDj < 0 || indexDj > 7){
            throw new IllegalArgumentException("indexDj out of range [0..7]");
        }

        // находится ли контейнер возле бункера?
        var currentPosition = getDPByDj(indexDj);
        if (!DPi[currentPosition]){
            throw new IllegalArgumentException("Container is not nearby this bunker!");
        }

        if (indexDj <= 3){
            MKL = DKL = true;
            MKR = DKR = false;
        }
        else {
            MKR = DKR = true;
            MKL = DKL = false;
        }
        DN = false;
    }

    private void rotateContainerToNormal(){
        DN = true;
        MKL = DKL = MKR = DKR = false;
    }

    private int getDPByDj(int indexDj){
        if (indexDj < 0 || indexDj > 7){
            throw new IllegalArgumentException("indexDP out of range [0..7]");
        }

        return switch (indexDj){
            case 0, 7 -> 0;
            case 1, 6 -> 1;
            case 2, 5 -> 2;
            case 3,4 -> 3;
            default -> throw new IllegalArgumentException("indexDj out of range [0..7]");
        };
    }

    private void setEmergencyStop(){
        EMERGENCY_STOP = true;
    }
    // endregion



    private ContainerState state;
    private int[] words;
    private boolean machineStopped;
    private boolean isContainerEmpty;   // изменить это в методе loading и unloading
    private int operationIndex;
    private int operationMask;
    private int currentDPi = -1;

    public void initialize(int[] words){
        if (words.length != 4){
            throw new IllegalArgumentException("words.length != 4");
        }

        this.words = words;
        operationIndex = -1;
        operationMask = -1;
        isContainerEmpty = true;
        machineStopped = false;
        // Перемещаю контейнер в P0
        state = ContainerState.PROCESS_CONTROL_CODE;
    }

    public void run(){
        while (!machineStopped){
            machineStopped = words[0] == -1 && words[1] == -1 && words[2] == -1 && words[3] == -1;
            switch (state){
                case INITIALIZING -> {continue;}
                case PROCESS_CONTROL_CODE -> processControlCode();
                case MOVING -> moving();
            }
        }
    }

    // region PROCESS_CODE_OPERATION methods methods
    private void processControlCode(){
        if (!canDoneCurrentOperation()){
            findNextOperation();
            return;
        }

        // закончил я на том, что нужно внимательно посмотреть код первого состояния, определить, где обнулять переменные
        IO.println("--- PROCESSING CONTROL CODE\t--- operation index: " + operationIndex + " execution code: " + operationMask);
        executeOperation();
        IO.println();
    }

    private boolean canDoneCurrentOperation(){
        // нет операции для завершения, ищем новую ИЛИ если операция полностью отработана, ищем новую
        return (operationMask != -1) && (operationMask != 0 || words[operationIndex] == 0);
    }

    private void findNextOperation(){
        // первое вхождение после инициализации
        if (operationIndex == -1){
            operationIndex = 0;
            operationMask = words[operationIndex];
            return;
        }

        try {
            while (words[operationIndex] == -1){
                operationIndex++;
            }
            operationMask = words[operationIndex];
        } catch (ArrayIndexOutOfBoundsException e){
            IO.println("Operation Index out of range [0..3]. So, there is no available operation to execute. Stopping machine...");
            machineStopped = true;
            //throw new IllegalArgumentException("Operation Index out of range [0..3]");
        }
    }

    private void executeOperation(){
        // проверять в самом начале код 000
        if (operationMask == 0 && words[operationIndex] == 0){
            if (!isContainerEmpty){
                IO.println("--- EXECUTION CODE 000 STATUS: CONTAINER IS NOT EMPTY, GOING TO THE UTILIZATION BUNKER (D0)");
                currentDPi = 0;
                state = ContainerState.MOVING;
                return;
            }
        }

        var isLoading = operationIndex % 2 == 0;
        var operationType = isLoading ? "LOADING" : "UNLOADING";
        var binOp = Integer.toBinaryString(words[operationIndex]);
        if (binOp.length() > 2)
            binOp = binOp.substring(binOp.length() - 3);
        var statusInfo = "--- EXECUTION CODE " + binOp + " STATUS: ";

        IO.println("--- EXECUTION CONTROL CODE\t--- operation index: " + operationIndex + " execution code: " + binOp + " status: " + operationType);

        if (isLoading){
            if (operationMask == 0 && words[operationIndex] == 0){
                IO.println("--- EXECUTION CODE 000 STATUS: SKIP OPERATION... NOTHING TO EXECUTE");
                completeOperation();
                return;
            }

            // Если есть Ri, то начинаем с него и сразу показываем, что её больше не делать в следующем цикле
            for (int i = 0; i < 3; i++) {
                if (isBit(operationMask, i)){
                    operationMask = setBit(operationMask, i, false);
                    currentDPi = i + 1;
                    IO.println(statusInfo + "MOVING TO LOAD IN R" + currentDPi);
                    state = ContainerState.MOVING;
                    break;
                }
            }

            // completeOperation(); вызвать эту дрисню в состоянии loading
        } else {
            if (isContainerEmpty){
                IO.println(statusInfo + "SKIP OPERATION... EMPTY CONTAINER");
                completeOperation();
                return;
            }

            currentDPi = operationMask;
            IO.println(statusInfo + "MOVING TO THE BUNKER: " + currentDPi);
            state = ContainerState.MOVING;
            // completeOperation(); вызвать эту дрисню в состоянии unoading
        }
    }

    /// Вызов или только после обработки
    private void completeOperation() {
        if (operationIndex < 0 || operationIndex > 3)
            return;

        words[operationIndex] = -1;
        operationMask = -1;
        currentDPi = -1;
    }
    // endregion

    // region MOVING methods
    private void moving(){
        //System.exit(1);
        state = ContainerState.PROCESS_CONTROL_CODE;
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
