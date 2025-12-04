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
    private boolean[] DPi = new boolean[] {true, false, false, false};   // Находится ли контейнер в позиции P0...P3
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


    // Убрать ненужные ошибки, которые ломают автомат
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

        Vi[valveIndex] = DVi[valveIndex] = false;
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

        int activeDPi = getActiveDPi();

        if (indexDP > activeDPi && !BK1){
            activeDPi += 1;
            setDPi(activeDPi);
            MR = true;
            ML = false;
        } else if (indexDP < activeDPi && !BK0){
            activeDPi -= 1;
            setDPi(activeDPi);
            ML = true;
            MR = false;
        } else {
            setEmergencyStop();
        }
    }

    private int getActiveDPi() {
        // получим индекс DP, где сейчас контейнер
        int activeDPi = -1;
        for (int i = 0; i < DPi.length; i++) {
            if (DPi[i]){
                activeDPi = i;
                break;  // причем он только один из 4
            }
        }
        return activeDPi;
    }

    private void setDPi(int indexDP){
        if (indexDP < 0 || indexDP > 3){
            throw new IllegalArgumentException("indexDP out of range [0..3]");
        }

        DPi[0] = DPi[1] = DPi[2] = DPi[3] = false;
        DPi[indexDP] = true;
    }

    private void rotateContainerToBunker(int indexDj){
        if (indexDj < 0 || indexDj > 7){
            throw new IllegalArgumentException("indexDj out of range [0..7]");
        }

        // находится ли контейнер возле бункера?
        var currentPosition = getDPiByDj(indexDj);

        if (!DPi[currentPosition]){
            throw new IllegalArgumentException("Container is not nearby this bunker!");
        }

        if (Dj[indexDj]){
            cannotUnload = true;
            return;
            //throw new IllegalArgumentException("Bunker is full!");
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

    private static int getDPiByDj(int indexDj) {
        return switch (indexDj){
            case 0, 7 -> 0;
            case 1, 6 -> 1;
            case 2, 5 -> 2;
            case 3,4 -> 3;
            default -> throw new IllegalArgumentException("indexDj out of range [0..7]");
        };
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
    private ContainerState state;
    private int operationIndex;
    private LoadStruct loadStruct;
    private int currentDPi;
    private boolean cannotUnload;   // все ведет к добавлению cannotUnload

    public AutomaticMachineCore(int[] words){
        if (words.length != 4){
            throw new IllegalArgumentException("words.length != 4");
        }

        this.words = words;
    }

    public void run(){
        while (!allOperationsCompleted() || operationIndex < 4){   // все УК == -1: все операции отработаны
            switch (state){
                case IDLE -> idle();
                case PROCESS_CONTROL_CODE -> processControlCode();
                case MOVE_TO_POSITION -> moveToPosition();
                // Загрузка:

                // Разгрузка:
                case ROTATE_TO_BUNKER -> rotateToBunker();
                case UNLOAD -> unload();
                case ROTATE_TO_NORMAL -> rotateToNormal();

                case COMPLETE_OPERATION -> completeOperation();

                case EMERGENCY_STOP -> emergencyStop();
                case END -> end();
                case null -> initialize();
                default -> throw new RuntimeException("unqe ungrrr");
            }
        }
    }



    // region AutomaticMachineMethods
    // Начальные методы
    private void initialize(){
        operationIndex = currentDPi = -1;
        loadStruct = null;
        state = ContainerState.IDLE;
    }

    private void idle(){
        state = allOperationsCompleted() || operationIndex >= 3 ? ContainerState.END : ContainerState.PROCESS_CONTROL_CODE;
    }

    private boolean allOperationsCompleted(){
        return words[0] == -1 && words[1] == -1 && words[2] == -1 && words[3] == -1;
    }

    // Методы циклы
    // Сейчас цель: закончить разгрузку полностью
    private void processControlCode(){
        // условие на довыполнение операции разгрузки будет позже

        operationIndex++;
        if (operationIndex % 2 == 0){   // ЗАГРУЗКА
            IO.println("ОПЕРАЦИИ ЗАГРУЗКИ ПОКА НЕДОСТУПНЫ, ПРОПУСК УПРАВЛЯЮЩЕГО КОДА!");
            return;
        } else {    // ВЫГРУЗКА
            currentDPi = getDPiByDj(words[operationIndex]);
        }

        state = ContainerState.MOVE_TO_POSITION;
    }

    private void moveToPosition(){
        moveTransporter(currentDPi);
        if (currentDPi == getActiveDPi())
            state = operationIndex % 2 == 0 ? ContainerState.OPEN_VALVE : ContainerState.ROTATE_TO_BUNKER;
    }

    // Загрузка

    // Выгрузка
    private void rotateToBunker(){
        rotateContainerToBunker(words[operationIndex]);

        // придется сбросить в отходы, если бункер переполнен)
        if (cannotUnload){
            cannotUnload = false;
            IO.println("Тот бункер заполнен, скидываю в отходы)");
            currentDPi = 0;
            words[operationIndex] = 0;  // ахахахах))))
            state = ContainerState.MOVE_TO_POSITION;
            return;
        }

        state = ContainerState.UNLOAD;
    }

    private void unload(){
        IO.println("Содержимое успешно выгружено!");
        state = ContainerState.ROTATE_TO_NORMAL;
    }

    private void rotateToNormal(){
        rotateContainerToNormal();
        state = ContainerState.COMPLETE_OPERATION;
    }



    private void completeOperation(){
        cannotUnload = false;
        currentDPi = -1;
        words[operationIndex] = -1;
        if (operationIndex % 2 == 0)
            loadStruct = null;
        state = ContainerState.IDLE;
    }



    // Методы завершения работы
    private void emergencyStop(){
        state = ContainerState.END;
    }

    private void end(){
        if (!EMERGENCY_STOP){
            IO.println("Конечный автомат завершил свою работу!");
            System.exit(1337);
        } else {
            IO.println("Аварийная остановка конечного автомата!");
            System.exit(1488);
        }
    }
    // endregion
}
