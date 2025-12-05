void main() {
    // ДЛЯ ПОИСКА ГДЕ МОЖНО СДЕЛАТЬ ПОДЛЯНКУ В ПОИСКЕ ТАК И НАПИСАТЬ "ПОДЛЯНК")))

    // подлянку можно сделать при изменении условий
    // по дефолту 0b101, 0b101, 0b011, 0b010
    var automaticMachine = new AutomaticMachineCore(new int[]{0b101, 0b101, 0b011, 0b010});
    automaticMachine.run();
}
