public class LoadStruct {
    public int controlCode;
    public int operationMask;
    public boolean cannotLoad;// все ведет к добавлению cannotLoad

    public LoadStruct(int controlCode){
        if (controlCode == 0)
            throw new IllegalArgumentException("Недопустимая операция. Невозможно создать LoadStruct с нулевым кодом!");

        this.controlCode = controlCode;
        this.operationMask = controlCode;
    }

    public boolean isOperationCompleted(){
        return controlCode != 0 && operationMask == 0;
    }

    public boolean canCompleteOperation(){
        return !cannotLoad;
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

        operationMask = setBit(operationMask, indexDP - 1, false);
    }

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
