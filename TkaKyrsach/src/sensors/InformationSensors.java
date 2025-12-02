package sensors;

import states.ContainerRotateDirection;

import java.util.List;

public class InformationSensors {
    private boolean[] DRi = new boolean[] {false, false, false};  // Сработал ли датчик пустоты в резервуаре Ri
    private boolean[] DVi = new boolean[] {false, false, false};  // Открыт ли резервуар Ri
    private boolean[] DPi = new boolean[] {true, false, false, false};   // Находится ли контейнер в позиции P0...P3
    private boolean DKL = false; // Поворачивается ли контейнер влево
    private boolean DKR = false; // Поворачивается ли контейнер вправо
    private boolean DN = true;   // В нейтральном ли положении контейнер
    private boolean[] Dj = new boolean[8];   // Заполнен ли бункер Dj
    private boolean BK0 = false; // Вышел ли контейнер за заданный левый предел
    private boolean BK1 = false; // Вышел ли контейнер за заданный правый предел
    private boolean DT = false;  // Сработал ли таймер заполнения
    private boolean AT = false;  // Сработал ли аварийный сигнал от таймера превышения ожидаемого времени операции

    private ControlSensors controlSensors;

    public void setControlSensors(ControlSensors controlSensors){
        this.controlSensors = controlSensors;
    }

    /// Получить состояния датчика резервуара
    public boolean getDRi(int index){
        if (index < 0 || index > 2)
            throw new IllegalArgumentException("Index out of array boundaries!");

        return DRi[index];
    }

    /// В целом, это для установки датчика полноты в false (материал в резервуаре закончился)
    public void setDRi(int index, boolean value){
        if (index < 0 || index > 2)
            throw new IllegalArgumentException("Index out of array boundaries!");

        DRi[index] = value;
    }



    public boolean getDVi(int index){
        if (index < 0 || index > 2)
            throw new IllegalArgumentException("Index out of array boundaries!");

        return DVi[index];
    }

    public void setDVi(int index, boolean value){
        if (index < 0 || index > 2)
            throw new IllegalArgumentException("Index out of array boundaries!");

        DVi[index] = value;
    }



    public boolean getDPi(int index){
        if (index < 0 || index > 3)
            throw new IllegalArgumentException("Index out of array boundaries!");

        return DPi[index];
    }

    public void setDPi(int index, boolean value){
        if (index < 0 || index > 3)
            throw new IllegalArgumentException("Index out of array boundaries!");

        DPi[0] = DPi[1] = DPi[2] = DPi[3] = false;
        DPi[index] = value;
    }

    public int getActiveDPi(){
        for (int i = 0; i < DPi.length; i++) {
            if (DPi[i])
                return i;
        }

        return -1;
    }



    public boolean getDKL(){
        return DKL;
    }

    public boolean getDKR(){
        return DKR;
    }

    public boolean getDN(){
        return DN;
    }

    public void setContainerRotation(ContainerRotateDirection direction){
        switch (direction){
            case ContainerRotateDirection.NORMAL -> {
                DKL = DKR = false;
                DN = true;
            }
            case ROTATE_LEFT -> {
                DKR = DN = false;
                DKL = true;
            }
            case ROTATE_RIGHT -> {
                DKL = DN = false;
                DKR = true;
            }
            case null, default -> throw new IllegalArgumentException("ContainerRotateDirection's value cannot be null!");
        }
    }



    public boolean getDj(int index){
        if (index < 0 || index > 7)
            throw new IllegalArgumentException("Index out of array boundaries!");

        return Dj[index];
    }

    public void setDj(int index, boolean value){
        if (index < 0 || index > 7)
            throw new IllegalArgumentException("Index out of array boundaries!");

        Dj[index] = value;
    }



    public boolean getBK0() {
        return BK0;
    }

    public void setBK0(boolean value) {
        BK0 = value;
    }

    public boolean getBK1() {
        return BK1;
    }

    public void setBK1(boolean value) {
        BK1 = value;
    }



    public boolean getDT() {
        return DT;
    }

    public void setDT(boolean value) {
        DT = value;
    }

    public boolean getAT() {
        return AT;
    }

    public void setAT(boolean value) {
        AT = value;
    }
}
