import tkinter as tk
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg


# Класс GaloisLFSR - линейный регистр сдвига с обратной связью Галуа
class GaloisLFSR:
    def __init__(self, taps, seed):
        self.taps = taps
        self.state = seed

    def shift(self):
        feedback = sum(self.state[tap] for tap in self.taps) % 2
        self.state = self.state[1:] + [feedback]

    def generate_sequence(self, length):
        sequence = []
        for _ in range(length):
            sequence.append(self.state[0])
            self.shift()
        return sequence


# Обновляет график на основе введенного начального значения
# сдвигового регистра и положений обратной связи
def update_graph():
    subplot.cla()  # Clear the previous plot
    seed_array = [int(bit) for bit in entrySeed.get()]
    taps_array = list(map(int, entryTaps.get().split()))
    lfsr = GaloisLFSR(taps_array, seed_array)
    bit_sequence = lfsr.generate_sequence(150)
    x = range(len(bit_sequence))
    y = bit_sequence

    chi_squared_test(bit_sequence)

    # subplot.figure()
    subplot.plot(x, y, marker='o', linestyle='', color='b', markersize=1)

    subplot.set_xlabel("X")
    subplot.set_ylabel("Y")
    subplot.set_title("Updated Plot")
    canvas.draw()


# Тестирование последовательности битов на случайность
# с использованием хи-квадратного теста и вывод результатов
def chi_squared_test(bit_sequence):
    # Шаг 1: Расчет наблюдаемых частот
    observed_zeros = sum(bit == 0 for bit in bit_sequence)
    observed_ones = len(bit_sequence) - observed_zeros

    # Шаг 2: Расчет ожидаемых частот при предположении случайности
    total_bits = len(bit_sequence)
    expected_zeros = total_bits / 2
    expected_ones = total_bits / 2

    # Шаг 3: Расчет значения статистики хи-квадрат
    chi_squared = ((observed_zeros - expected_zeros) ** 2) / expected_zeros
    chi_squared += ((observed_ones - expected_ones) ** 2) / expected_ones

    # Шаг 4: Степени свободы
    degrees_of_freedom = 1

    # Шаг 5: Сравнение со значением критического значения
    critical_value = 3.841  # For a significance level of 0.05 and 1 degree of freedom

    # Вывод результатов
    print("Observed Zeros:", observed_zeros)
    print("Observed Ones:", observed_ones)
    print("Expected Zeros:", expected_zeros)
    print("Expected Ones:", expected_ones)
    print("Chi-Squared:", chi_squared)
    print("Degrees of Freedom:", degrees_of_freedom)
    print("Critical Value:", critical_value)

    if chi_squared < critical_value:
        print("The bit sequence is likely random.")
    else:
        print("The bit sequence is not likely random.")


# Шифрует файл BMP с использованием операции XOR и
# сохраняет зашифрованный файл с указанным именем
def xor_cipher_bmp_file(filename, key_seed, taps):
    with open(filename, 'rb') as file:
        bmp_data = file.read()

    header = bmp_data[:110]  # BMP header is 110 bytes
    image_data = bytearray(bmp_data[110:])

    key_length = len(image_data)
    lfsr = GaloisLFSR(taps, key_seed)  # Пример положений обратной связи: 2, 3 и 5
    key_sequence_bit = lfsr.generate_sequence(key_length * 8)
    key_sequence = [sum([byte[b] << b for b in range(0, 8)])
                    for byte in zip(*(iter(key_sequence_bit),) * 8)]
    for i in range(key_length):
        image_data[i] ^= key_sequence[i]

    encrypted_bmp_data = header + bytes(image_data)

    with open('encrypted.bmp', 'wb') as file:
        file.write(encrypted_bmp_data)

    print("Encryption completed. Encrypted BMP saved as 'encrypted.bmp'.")


filename = 'tux.bmp'


# Зашифровывает файл BMP с использованием
# введенного начального значения сдвигово регистра
# и положений обратной связи
def encrypt_file():
    seed_array = [int(bit) for bit in entrySeed.get()]
    taps_array = list(map(int, entryTaps.get().split()))
    xor_cipher_bmp_file(filename, seed_array, taps_array)


# Создание окна tkinter
window = tk.Tk()
window.title("dp_Lab5_py")

# Создание фигуры matplotlib
figure = Figure(figsize=(6, 4), dpi=100)
subplot = figure.add_subplot(111)

# Создание холста для отображения фигуры matplotlib в tkinter
canvas = FigureCanvasTkAgg(figure, master=window)
canvas.draw()
canvas.get_tk_widget().pack(side=tk.LEFT)

# Создание фрейма для кнопки и полей ввода
controls_frame = tk.Frame(window)
controls_frame.pack(side=tk.RIGHT)

label1 = tk.Label(controls_frame, text="Enter seed:")
label1.pack(anchor=tk.W)  # Выравнивание метки слева

entrySeed = tk.Entry(controls_frame)
entrySeed.pack()

label2 = tk.Label(controls_frame, text="Enter taps:")
label2.pack(anchor=tk.W)  # Выравнивание метки слева

entryTaps = tk.Entry(controls_frame)
entryTaps.pack()

button = tk.Button(controls_frame, text="Generate graph", command=update_graph)
button.pack()

# Создание кнопки для обновления графика
button = tk.Button(controls_frame, text="Encrypt file", command=encrypt_file)
button.pack()

# Запуск цикла событий tkinter
tk.mainloop()
