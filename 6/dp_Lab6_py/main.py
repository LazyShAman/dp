import tkinter as tk
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import numpy as np

intro = True


# Определение функции для интро
def update_interface():
    if intro:
        global up
        global a
        if not up:
            if a > -40:
                a -= 2
            else:
                up = True
        else:
            if a < 40:
                a += 2
            else:
                up = False
        generate_graphV(a, 0)
        # Обновление каждые 1000 milliseconds (1 second)
        window.after(50, update_interface)


# Определение функции эллиптической кривой
def f(x, a, b):
    return x ** 3 + a * x + b


# Определение функции для сложения двух точек
def sum_two_points(x1, y1, x2, y2, a, b):
    kl = (y2 - y1) / (x2 - x1)
    bl = -x1 * kl + y1  # bl = -x2*kl + y2

    # y^2 = x^3 + ax + b, y = kl * x + bl => [-1, kl^2, 2 * kl * bl, bl^2 - b]
    poly = np.poly1d([-1, kl ** 2, 2 * kl * bl, bl ** 2 - b])

    # Корни уравнения
    x = np.roots(poly)
    y = np.sqrt(f(x, a, b))
    return x, y, kl, bl


# Определение функции для удвоения точки
def double_point(x, y, a, b):
    kl = (3 * x ** 2 + a) / (2 * y)
    bl = -x * kl + y

    # y^2 = x^3 + ax + b, y = kl * x + bl => [-1, kl^2, 2 * kl * bl, bl^2 - b]
    poly = np.poly1d([-1, kl ** 2, 2 * kl * bl - a, bl ** 2 - b])

    # Корни уравнения
    x = np.roots(poly)
    y = np.sqrt(f(x, a, b))
    return x, y, kl, bl


# Определение функции для выполнения операций
def calc_points():
    a, b = get_params()

    # Точка 1
    x1 = int(entryX1.get())
    y1 = -np.sqrt(f(x1, a, b))

    # Точка 2
    x2 = int(entryX2.get())
    y2 = np.sqrt(f(x2, a, b))

    # Линия: y = kl * x + bl
    if x1 != x2:
        x, y, kl, bl = sum_two_points(x1, y1, x2, y2, a, b)
    else:
        x, y, kl, bl = double_point(x1, y1, a, b)

    scaling = max(int(max(x) * 2), int(max(y) * 2))
    generate_graph(scaling, scaling)
    subplot.plot(x, y, "o")
    subplot.plot(x, -y, "o")

    x = np.linspace(min(x), max(x))
    subplot.plot(x, kl * x + bl)
    canvas.draw()


# Определение функции получения параметров
def get_params():
    a = int(entryA.get())
    b = int(entryB.get())
    return a, b


# Определение функции для генерации графа
def generate_graph(scaleX=25, scaleY=25):
    global intro
    intro = False
    subplot.cla()  # Очистка предыдущего графика

    a, b = get_params()
    Y, X = np.mgrid[-scaleX:scaleX:250j, -scaleY:scaleY:250j]
    print(scaleX, scaleY)
    subplot.contour(X, Y, Y ** 2 - f(X, a, b), levels=[0])

    subplot.set_xlabel("X")
    subplot.set_ylabel("Y")
    subplot.set_title("График эллиптической кривой")
    canvas.draw()
    return a, b


# Определение функции для обновления графа
def generate_graphV(_a, _b):
    subplot.cla()  # Очистка предыдущего графика
    a = _a
    b = _b

    Y, X = np.mgrid[-25:25:100j, -25:25:100j]
    subplot.contour(X, Y, Y ** 2 - f(X, a, b), levels=[0])

    subplot.set_xlabel("X")
    subplot.set_ylabel("Y")
    subplot.set_title("График эллиптической кривой")
    canvas.draw()


# Создание окна tkinter
window = tk.Tk()
window.title("dp_Lab6_py")

# Создание объекта Figure для matplotlib
figure = Figure(figsize=(6, 4), dpi=100)
subplot = figure.add_subplot(111)

# Создание холста для отображения графика matplotlib в tkinter
canvas = FigureCanvasTkAgg(figure, master=window)
canvas.draw()

canvas.get_tk_widget().pack(side=tk.LEFT)

# Создание фрейма для кнопки и полей ввода
controls_frame = tk.Frame(window)
controls_frame.pack(side=tk.RIGHT)

# Создание метки и поля ввода для параметра 'a'
label1 = tk.Label(controls_frame, text="Введите a:")
label1.pack(anchor=tk.W)  # Выравнивание метки слева

entryA = tk.Entry(controls_frame)
entryA.pack()

# Создание метки и поля ввода для параметра 'b'
label2 = tk.Label(controls_frame, text="Введите b:")
label2.pack(anchor=tk.W)  # Выравнивание метки слева

entryB = tk.Entry(controls_frame)
entryB.pack()

# Создание кнопки для генерации графика
button = tk.Button(controls_frame, text="Сгенерировать график", command=generate_graph)
button.pack()

# Создание первого поля ввода
entryX1 = tk.Entry(controls_frame)
entryX1.pack(pady=(50, 0))

# Создание второго поля ввода
entryX2 = tk.Entry(controls_frame)
entryX2.pack()

# Создание кнопки для произведения операций над выбранными точками
button = tk.Button(controls_frame, text="Рассчитать", command=calc_points)
button.pack()

a = 15
up = False

# Запуск цикла обработки событий tkinter
update_interface()
tk.mainloop()
