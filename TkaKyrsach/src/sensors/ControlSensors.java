package sensors;

import states.ContainerRotateDirection;
import states.TransporterMoveDirection;

public class ControlSensors {
    private boolean[] Vi = new boolean[3];    // Открыть (true) или закрыть (false) задвижку резервуаров
    private boolean MR = false;   // Транспортер вправо 0-1
    private boolean ML = false;   // Транспортер влево 0-1
    private boolean MKR = false;  // Повернуть контейнер вправо для сброса в B0-B3 0-1
    private boolean MKL = false;  // Повернуть контейнер влево для сброса в B4-B7 0-1
    private boolean EMERGENCY_STOP = false;   // Аварийная остановка транспортера

    private InformationSensors informationSensors;

    public void setInformationSensors(InformationSensors informationSensors){
        this.informationSensors = informationSensors;
    }

    public void openValve(int index){
        if (index < 0 || index > 2)
            return;

        if (!informationSensors.getDVi(index)){
            Vi[index] = true;
            informationSensors.setDVi(index, true);
        }
    }

    public void closeValve(int index){
        if (index < 0 || index > 2)
            return;

        if (informationSensors.getDVi(index)){
            Vi[index] = false;
            informationSensors.setDVi(index, false);
        }
    }

    public void moveTransporter(TransporterMoveDirection direction){
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

    public void rotateTransporter(ContainerRotateDirection direction){
        informationSensors.setContainerRotation(direction);
        switch (direction){
            case NORMAL:
                MKR = false;
                MKL = false;
                break;
            case ROTATE_LEFT:
                MKR = false;
                MKL = true;
                break;
            case ROTATE_RIGHT:
                MKR = true;
                MKL = false;
                break;
        }
    }

    public void processEmergencyStop(){
        EMERGENCY_STOP = true;
    }

    public boolean getEmergencyStop(){
        return EMERGENCY_STOP;
    }
}
