import java.util.Scanner;

public class StateController {
    static void main() {
        // ДЛЯ ПОИСКА ГДЕ МОЖНО СДЕЛАТЬ ПОДЛЯНКУ В ПОИСКЕ ТАК И НАПИСАТЬ "ПОДЛЯНК")))

        // подлянку можно сделать при изменении условий
        // по дефолту 0b101, 0b101, 0b011, 0b010

        IO.println("Введите 4 управляющих кода через пробел! Пример:\n101 101 011 010");

        var scanner = new Scanner(System.in);
        var line = scanner.nextLine();
        var rawCodes = line.split(" ");
        if(line.isEmpty() || rawCodes.length != 4)
            throw new RuntimeException("Введите 4 управляющих кода через пробел! Пример:\n101 101 011 010");

        var codes = new int[4];
        for (int i = 0; i < rawCodes.length; i++) {
            var num = Integer.parseInt(rawCodes[i], 2);
            if (num > 7)
                throw new IllegalArgumentException("Число должно состоять из 3-х разрядов!\nИли же быть меньше 8 в 10чной СС!");
            codes[i] = num;
        }

        IO.println();
        var automaticMachine = new AutomaticMachineCore(codes);
        automaticMachine.run();
    }
}
