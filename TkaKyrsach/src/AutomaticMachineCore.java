public class AutomaticMachineCore {
    // открыть (true) или закрыть (false) задвижку резервуаров
    private final boolean[] Vi = new boolean[3];
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
    private final boolean[] DRi = new boolean[] {false, false, false};  // Сработал ли датчик пустоты в резервуаре Ri
    // false - закрыт, true - открыт
    private final boolean[] DVi = new boolean[] {false, false, false};  // Открыт ли клапан резервуара Ri
    private final boolean[] DPi = new boolean[] {true, false, false, false};   // Находится ли контейнер в позиции P0...P3
    private boolean DKL = false; // Поворачивается ли контейнер влево
    private boolean DKR = false; // Поворачивается ли контейнер вправо
    private boolean DN = true;   // В нейтральном ли положении контейнер
    // false - не заполнен, true - заполнен
    private final boolean[] Dj = new boolean[8];   // Заполнен ли бункер Dj
    private boolean BK0 = false; // Вышел ли контейнер за заданный левый предел
    private boolean BK1 = false; // Вышел ли контейнер за заданный правый предел
    private boolean DT = false;  // Сработал ли таймер заполнения
    private boolean AT = false;  // Сработал ли аварийный сигнал от таймера превышения ожидаемого времени операции



    private final int OPERATION_TIME = 10;
    private final int EMERGENCY_OPERATION_TIME = 15;



    enum ContainerState {
        INIT,
        IDLE,
        PROCESS_CONTROL_CODE,
        MOVE_TO_POSITION,
        OPEN_VALVE,
        WAIT_TIMER,
        CLOSE_VALVE,
        ROTATE_TO_BUNKER,
        UNLOAD,
        ROTATE_TO_NORMAL,
        COMPLETE_OPERATION,
        EMERGENCY_STOP,
        END
    }



    // region Методы конечного автомата
    private void openValve(int destinationPointIndex){
        var valveIndex = getReservoirIndex(destinationPointIndex);
        Vi[valveIndex] = DVi[valveIndex] = true;
    }

    private void closeValve(int destinationPointIndex){
        var valveIndex = getReservoirIndex(destinationPointIndex);
        Vi[valveIndex] = DVi[valveIndex] = false;
    }

    private void moveTransporter(int destinationPointIndex){
        checkDestinationPointIndex(destinationPointIndex);

        // может быть, транспортер уже под DPi?
        if (isContainerOnDestinationPoint(destinationPointIndex)){
            MR = ML = false;
            return;
        }

        int activeDestinationPointIndex = getActiveDestinationPointIndex();

        if (destinationPointIndex > activeDestinationPointIndex && !BK1){
            activeDestinationPointIndex += 1;
            setDPi(activeDestinationPointIndex);
            MR = true;
            ML = false;
        } else if (destinationPointIndex < activeDestinationPointIndex && !BK0){
            activeDestinationPointIndex -= 1;
            setDPi(activeDestinationPointIndex);
            ML = true;
            MR = false;
        } else {
            setEmergencyStop();
        }
    }

    private void rotateContainerToBunker(int bunkerIndex){
        if (bunkerIndex <= 3){
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

    private void setEmergencyStop(){
        EMERGENCY_STOP = true;
    }
    // endregion



    private int[] words;
    private ContainerState state = ContainerState.INIT;
    private int operationIndex = 0; // раньше начиналось с -1
    private LoadStruct loadStruct = null;
    private int currentDestinationPointIndex = -1;

    public AutomaticMachineCore(int[] words){
        if (words.length != 4){
            throw new IllegalArgumentException("words.length != 4");
        }

        this.words = words;
    }

    public void run(){
        IO.println(logInfo("-----\tИНИЦИАЛИЗАЦИЯ И ЗАПУСК КОНЕЧНОГО АВТОМАТА\t-----\n"));
        state = ContainerState.IDLE;

        // подляночки
        //Dj[2] = true;
        //DRi[0] = true;

        while (!canMachineBeStopped()) {
            switch (state) {
                case IDLE -> idle();
                case PROCESS_CONTROL_CODE -> processControlCode();
                case MOVE_TO_POSITION -> moveToPosition();
                case COMPLETE_OPERATION -> completeOperation();

                // Загрузка:
                case OPEN_VALVE -> openValve();
                case WAIT_TIMER -> waitTimer();
                case CLOSE_VALVE -> closeValve();

                // Разгрузка:
                case ROTATE_TO_BUNKER -> rotateToBunker();
                case UNLOAD -> unload();
                case ROTATE_TO_NORMAL -> rotateToNormal();

                // Ничего не делать
                case INIT, EMERGENCY_STOP, END -> {}
                case null, default -> throw new RuntimeException("unqe? ungrrr!");
            }
        }

        state = ContainerState.END;
        var message = logInfo(EMERGENCY_STOP ?
                "\033[0;31m-----\tАВАРИЙНАЯ ОСТАНОВКА КОНЕЧНОГО АВТОМАТА!\t-----\033[0m" : "-----\tКонечный автомат завершил свою работу!\t-----");
        IO.println(message);
    }



    // region AutomaticMachineMethods
    // Начальные методы
    private void idle(){
        state = canMachineBeStopped() ? ContainerState.END : ContainerState.PROCESS_CONTROL_CODE;
    }

    // Методы циклы
    private void processControlCode(){
        var message = String.format("-----\tОБРАБОТКА УПРАВЛЯЮЩЕГО КОДА \"%s\"\t-----", to3BitBinary(words[operationIndex]));
        IO.println(logDebug(message));

        if (loadStruct != null){
            processLoadOperation();
            return;
        }

        //operationIndex++; // при начале с -1 было раскомментированно
        //setEmergencyStop(); // вообще это просто для теста, можно спокойно удалить, подляночка

        // ЗАГРУЗКА
        if (operationIndex % 2 == 0){
            processLoadOperation();
            return;
        }

        // ВЫГРУЗКА
        currentDestinationPointIndex = getContainerPositionByBunker(words[operationIndex]);
        state = ContainerState.MOVE_TO_POSITION;
    }

    private void moveToPosition(){
        var message = String.format("-----\tПЕРЕМЕЩЕНИЕ КОНТЕЙНЕРА К DP_%d\t-----", currentDestinationPointIndex);
        IO.println(message);

        moveTransporter(currentDestinationPointIndex);
        if (currentDestinationPointIndex == getActiveDestinationPointIndex())
            state = operationIndex % 2 == 0 ? ContainerState.OPEN_VALVE : ContainerState.ROTATE_TO_BUNKER;  // Загрузка : Выгрузка
    }

    private void completeOperation(){
        IO.println(logInfo("-----\tОПЕРАЦИЯ ЗАВЕРШЕНА!\t-----"));

        if (operationIndex % 2 == 0 && loadStruct != null){
            loadStruct.updateExecutedLoadPosition(currentDestinationPointIndex);
            currentDestinationPointIndex = -1;
            if (loadStruct.isOperationCompleted()){
                loadStruct = null;
                words[operationIndex] = -1;
                operationIndex++;   // закомментить это если нужно вернуть -1
            }
        } else {
            currentDestinationPointIndex = -1;
            words[operationIndex] = -1;
            operationIndex++;   // закомментить это если нужно вернуть -1
        }

        state = ContainerState.IDLE;
        IO.println();
    }

    // Загрузка
    private void processLoadOperation(){
        // основная задача: получить индекс куда двигаться и перевести автомат в соответствующее состояние

        // начать стоит с проверки на нулевой управляющий код
        if (words[operationIndex] == 0){
            moveContainerToWasteBunkerInNextOperation();
            return;
        }

        // если до этого в этом цикле новая операция загрузки будет первой
        if (loadStruct == null){
            loadStruct = new LoadStruct(words[operationIndex]);
        }

        currentDestinationPointIndex = loadStruct.getNextLoadPosition();
        var reservoirIndex = getReservoirIndex(currentDestinationPointIndex);

        // на всякий пожарный
        checkDestinationPointIndex(currentDestinationPointIndex);
        checkReservoirIndex(reservoirIndex);

        if (isReservoirEmpty(reservoirIndex) || !isContainerInNeutralRotation() || isReservoirOpened(reservoirIndex)){
            moveContainerToWasteBunkerInNextOperation();
            return;
        }
        state = ContainerState.MOVE_TO_POSITION;
    }

    private void openValve(){
        var message = String.format("-----\tОТКРЫТИЕ КЛАПАНА РЕЗЕРВУАРА R_%s!\t-----", currentDestinationPointIndex);
        IO.println(message);
        openValve(currentDestinationPointIndex);
        state = ContainerState.WAIT_TIMER;
    }

    private void waitTimer(){
        // waiting...
        boolean causeErrorDirectly = false;
        runTimer(causeErrorDirectly);

        IO.println("-----\tЗАГРУЗКА В КОНТЕЙНЕР...\t-----");
        state = ContainerState.CLOSE_VALVE;
    }

    private void closeValve(){
        var message = String.format("-----\tЗАКРЫТИЕ КЛАПАНА РЕЗЕРВУАРА R_%s!\t-----", currentDestinationPointIndex);
        IO.println(message);

        if (isReservoirClosed(getReservoirIndex(currentDestinationPointIndex))){
            throw new IllegalArgumentException("Valve in this position is already closed!");
        }

        closeValve(currentDestinationPointIndex);
        state = ContainerState.COMPLETE_OPERATION;
    }



    // Выгрузка (меня полностью устраивает, до тестирования)
    private void rotateToBunker(){
        if (!canRotateContainerToBunker(words[operationIndex])){
            moveContainerToWasteBunkerInCurrentOperation();
            return;
        }

        var message = String.format("-----\tПОВОРОТ КОНТЕЙНЕРА К БУНКЕРУ D_%s ДЛЯ ВЫГРУЗКИ!\t-----", words[operationIndex]);
        IO.println(message);

        rotateContainerToBunker(words[operationIndex]);
        state = ContainerState.UNLOAD;
    }

    private void unload(){
        IO.println("-----\tСОДЕРЖИМОЕ УСПЕШНО ВЫГРУЖЕНО!\t-----");
        state = ContainerState.ROTATE_TO_NORMAL;
    }

    private void rotateToNormal(){
        rotateContainerToNormal();
        IO.println("-----\tКОНТЕЙНЕР ВОЗВРАЩЕН В НЕЙТРАЛЬНОЕ ПОЛОЖЕНИЕ!\t-----");
        state = ContainerState.COMPLETE_OPERATION;
    }
    // endregion



    // Типовые проверки
    private boolean allOperationsCompleted(){
        return words[0] == -1 && words[1] == -1 && words[2] == -1 && words[3] == -1;
    }

    private boolean canMachineBeStopped(){
        return allOperationsCompleted() || EMERGENCY_STOP;
    }

    private boolean isContainerOnDestinationPoint(int destinationPointIndex){
        checkDestinationPointIndex(destinationPointIndex);
        return DPi[destinationPointIndex];
    }

    private boolean isReservoirEmpty(int reservoirIndex){
        checkReservoirIndex(reservoirIndex);
        return DRi[reservoirIndex];
    }

    private boolean isContainerInNeutralRotation(){
        return DN;
    }

    private boolean isReservoirOpened(int reservoirIndex){
        checkReservoirIndex(reservoirIndex);
        return DVi[reservoirIndex];
    }

    private boolean isReservoirClosed(int reservoirIndex){
        checkReservoirIndex(reservoirIndex);
        return !DVi[reservoirIndex];
    }

    private boolean isBunkerFull(int bunkerIndex){
        checkBunkerIndex(bunkerIndex);
        return Dj[bunkerIndex];
    }

    private boolean canRotateContainerToBunker(int bunkerIndex){
        checkBunkerIndex(bunkerIndex);
        return isContainerOnDestinationPoint(getContainerPositionByBunker(bunkerIndex)) && !isBunkerFull(bunkerIndex);
    }



    // Типовые методы
    private void checkDestinationPointIndex(int destinationPointIndex){
        if (destinationPointIndex < 0 || destinationPointIndex > 3){
            throw new IllegalArgumentException("destinationPointIndex is out of range [0..3]");
        }
    }

    private void checkReservoirIndex(int reservoirIndex){
        if (reservoirIndex < 0 || reservoirIndex > 2){
            throw new IllegalArgumentException("indexR is out of range [0..2]");
        }
    }

    private void checkBunkerIndex(int bunkerIndex){
        if (bunkerIndex < 0 || bunkerIndex > 7){
            throw new IllegalArgumentException("bunkerIndex is out of range [0..7]");
        }
    }

    private int getReservoirIndex(int destinationPointIndex) {
        return switch (destinationPointIndex){
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            default -> throw new IllegalArgumentException("destinationPointIndex out of range [1..3]");
        };
    }

    private int getActiveDestinationPointIndex() {
        // получим индекс DP, где сейчас контейнер
        for (int i = 0; i < DPi.length; i++) {
            if (DPi[i]){
                return i;
            }
        }
        throw new IllegalStateException("There is no active DPi!");
    }

    private void setDPi(int destinationPointIndex){
        checkDestinationPointIndex(destinationPointIndex);
        DPi[0] = DPi[1] = DPi[2] = DPi[3] = false;
        DPi[destinationPointIndex] = true;
    }

    private static int getContainerPositionByBunker(int bunkerIndex) {
        return switch (bunkerIndex){
            case 0, 7 -> 0;
            case 1, 6 -> 1;
            case 2, 5 -> 2;
            case 3,4 -> 3;
            default -> throw new IllegalArgumentException("bunkerIndex out of range [0..7]");
        };
    }

    private void moveContainerToWasteBunkerInCurrentOperation(){
        var message = String.format("-----\tБУНКЕР D_%s ЗАПОЛНЕН! ОТПРАВЛЯЮ СОДЕРЖИМОЕ НА УТИЛИЗАЦИЮ!\t-----", words[operationIndex]);
        IO.println(logDebug(message));
        currentDestinationPointIndex = 0;
        words[operationIndex] = 0;
        state = ContainerState.MOVE_TO_POSITION;
    }

    private void moveContainerToWasteBunkerInNextOperation(){
        var message = "-----\tУПРАВЛЯЮЩИЙ КОД \"000\" ИЛИ ОШИБКА ПРИ ВЫПОЛНЕНИИ ОПЕРАЦИИ ЗАГРУЗКИ (ЗАКОНЧИЛСЯ МАТЕРИАЛ)!\t-----\n-----\tОТПРАВЛЯЮ СОДЕРЖИМОЕ НА УТИЛИЗАЦИЮ СО СЛЕДУЮЩЕЙ ОПЕРАЦИИ!\t-----";
        IO.println(logDebug(message));
        words[operationIndex + 1] = 0b000;  // ошибка индекса никогда не возникнет
        loadStruct = null;
        state = ContainerState.COMPLETE_OPERATION;
    }

    private void runTimer(boolean causeErrorDirectly){
        var elapsedTime = 0;
        if (!causeErrorDirectly){
            while (!DT){
                elapsedTime++;
                if (elapsedTime >= OPERATION_TIME){
                    DT = true;
                    IO.println(logDebug("Сработал датчик времени выполнения операции DT!"));
                    stopTimer();
                    break;
                }
            }
        } else {
            while (!AT){
                elapsedTime++;

                if (elapsedTime >= OPERATION_TIME){
                    DT = true;
                }
                if (elapsedTime >= EMERGENCY_OPERATION_TIME){
                    AT = true;
                    IO.println("Сработал датчик \033[0;31mАВАРИЙНОГО\033[0m времени выполнения операции AT!");
                    stopTimer();
                    break;
                }
            }
        }
    }

    private void stopTimer(){
        DT = AT = false;
    }

    public String to3BitBinary(int number) {
        // Ограничиваем число 3 битами (0-7)
        int maskedNumber = number & 0b111;
        // Форматируем с ведущими нулями
        return String.format("%3s", Integer.toBinaryString(maskedNumber))
                .replace(' ', '0');
    }

    public String logDebug(String logging) {
        return "\u001B[34m" + logging + "\u001B[0m";
    }
    public String logInfo(String logging) {
        return "\u001B[32m" + logging + "\u001B[0m";
    }
    public String logError(String logging) {
        return "\u001B[31m" + logging + "\u001B[0m";
    }
}
