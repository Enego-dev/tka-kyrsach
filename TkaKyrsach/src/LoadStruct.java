public class LoadStruct {
    public int controlCode;
    public int operationMask;

    public LoadStruct(int controlCode){
        if (controlCode == 0)
            throw new IllegalArgumentException("Недопустимая операция. Невозможно создать LoadStruct с нулевым кодом!");

        this.controlCode = controlCode;
        this.operationMask = controlCode;
    }

    public boolean isOperationCompleted(){
        return controlCode != 0 && operationMask == 0;
    }

    /**
     * @return may return -1
     */
    public int getNextLoadPosition(){
        for (int i = 0; i < 3; i++) {
            if (isBit(operationMask, i)){
                //operationMask = setBit(operationMask, i, false);
                return i + 1;
            }
        }

        return -1;
    }

    public void updateExecutedLoadPosition(int indexDP){
        if (indexDP < 1 || indexDP > 3)
            throw new IllegalArgumentException("Ожидалась позиция DPi[1..3]!");

        operationMask = setBit0(operationMask, indexDP - 1);
    }

    // region Методы для работы с битами
    private static boolean isBit(int word, int index){
        return (word & 1 << index) != 0;
    }

    private static int setBit0(int word, int index){
        word &= ~(1 << index);
        return word;
    }
    // endregion
}
