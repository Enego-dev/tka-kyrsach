import states.ContainerRotateDirection;
import states.ContainerState;
import states.TransporterMoveDirection;

public class StateController {
    // region Управляющие сигналы (сделать что-то)
    private static boolean[] Vi = new boolean[3];  // Открыть задвижку резервуаров -- индексирование с 0 до 2
    private static boolean MR = false; // Транспортер вправо 0-1
    private static boolean ML = false; // Транспортер влево 0-1
    private static boolean MKR = false;    // Повернуть контейнер вправо для сброса в B0-B3 0-1
    private static boolean MKL = false;    // Повернуть контейнер влево для сброса в B4-B7 0-1
    private static boolean EMERGENCY_STOP = false; // Аварийная остановка транспортера

    private static void openValve(int number){
        Vi[0] = false;
        DVi[0] = false;
        Vi[1] = false;
        DVi[1] = false;
        Vi[2] = false;
        DVi[2] = false;

        switch (number){
            case 1:
                Vi[0] = true;
                DVi[0] = true;
                break;
            case 2:
                Vi[1] = true;
                DVi[1] = true;
                break;
            case 3:
                Vi[2] = true;
                DVi[2] = true;
                break;
            default:
                throw new IllegalArgumentException("Ожидается 1, 2 или 3");
        }
    }

    private static void moveTransporter(TransporterMoveDirection direction){
        switch (direction){
            case IDLE:
                MR = false;
                ML = false;
                break;
            case MOVE_LEFT:
                MR = false;
                ML = true;
                break;
            case MOVE_RIGHT:
                MR = true;
                ML = false;
                break;
        }
    }

    private static void rotateTransporter(ContainerRotateDirection direction){
        switch (direction){
            case IDLE:
                MKR = false;
                MKL = false;

                DKL = false;
                DKR = false;
                DN = true;

                break;
            case ROTATE_LEFT:
                MKR = false;
                MKL = true;

                DKL = true;
                DKR = false;
                DN = false;

                break;
            case ROTATE_RIGHT:
                MKR = true;
                MKL = false;

                DKL = false;
                DKR = true;
                DN = false;

                break;
        }
    }

    private static void processEmergencyStop(){
        EMERGENCY_STOP = true;
    }
    // endregion

    // region Информационные сигналы (проверить что-то)
    private static boolean[] DRi = new boolean[3]; // Опустошен ли резервуар Ri -- индексирование с 0 до 2 -- реальное с 1 до 3
    private static boolean[] DVi = new boolean[3]; // Открыт ли резервуар Ri -- индексирование с 0 до 2 -- реальное с 1 до 3
    private static boolean[] DPi = new boolean[4]; // Находится ли контейнер под резервуаром Ri или он в P0 -- индексирование с 0 до 3 -- реальное с 0 до 3
    private static boolean DKL = false;    // Поворачивается ли контейнер влево
    private static boolean DKR = false;    // Поворачивается ли контейнер вправо
    private static boolean DN = true;  // В нейтральном ли положении контейнер
    private static boolean[] Dj = new boolean[8];  // Заполнен ли бункер Dj -- индексирование с 0 до 7 -- реальное с 0 до 7
    private static boolean BK0 = false;    // Вышел ли контейнер за заданный левый предел
    private static boolean BK1 = false;    // Вышел ли контейнер за заданный правый предел
    private static boolean DT = false; // Сработал ли таймер заполнения
    private static boolean AT = false; // Сработал ли аварийный сигнал от таймера превышения ожидаемого времени операции
    // endregion

    // region Технические переменные
    private static ContainerState containerState = ContainerState.IDLE;

    private static byte fillTimer = 15;    // таймер заполнения на 15 секунд
    private static byte emergencyTimer = 20;   // аварийный таймер операции на 20 секунд

    // Положение:
    // <0 - аварийная точка BK0
    // движение контейнера в 0-50, где 10 - P0, 20 - P1, 30 - P2, 40 - P3
    // >50 - аварийная точка BK1
    private static short containerPosition = 5;
    // endregion

    static void main() {
        while (true){
            if(shouldStayInIdle()){
                containerState = ContainerState.IDLE;
                continue;
            }

            switch (containerState){
                case IDLE -> {break;}
                case MOVING_TO_LOAD -> {movingToLoad(); break;}
                case LOADING -> {loading(); break;}
                case MOVING_TO_UNLOAD -> {movingToUnload(); break;}
                case UNLOADING -> {unloading(); break;}
                case EMERGENCY_STOP -> {emergencyStop(); break;}
                case null, default -> {throw new IllegalStateException("Неверное состояние автомата!");}
            }
        }
    }

    private static void movingToLoad(){

    }

    private static void loading(){

    }

    private static void movingToUnload(){

    }

    private static void unloading(){

    }

    private static void emergencyStop(){

    }

    // region Проверки на доступность выполнение хотя бы одной операции
    public static boolean shouldStayInIdle() {
        boolean cannotOp1 = Dj[5] || DRi[0] || DRi[2];  // Нельзя выполнить операцию1
        boolean cannotOp2 = Dj[2] || DRi[0] || DRi[1];  // Нельзя выполнить операцию2

        return cannotOp1 && cannotOp2;  // Нельзя выполнить НИ ОДНУ операцию
    }
    // endregion
}
