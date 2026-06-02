# 🌱 StudyBuddy Desktop

> **Gamificación y productividad para estudiantes que pasan demasiadas horas frente al PC.**
> Proyecto Final de Grado (TFG) - 2º DAM (Desarrollo de Aplicaciones Multiplataforma).

StudyBuddy es una aplicación de escritorio diseñada para combatir la procrastinación digital. A diferencia de un simple cronómetro, esta herramienta transforma el tiempo de estudio en recursos (Oro y Gotas de Agua) para cuidar y evolucionar un ecosistema virtual. Si estudias, tu planta crece; si abandonas la sesión o pasas días sin conectarte, la planta se marchita.

La idea nace de una necesidad real: mantener el foco cuando el entorno de trabajo (el ordenador) es también la mayor fuente de distracciones, aplicando mecánicas de "aversión a la pérdida".

## 🛠️ Stack Tecnológico

El proyecto está construido aplicando una arquitectura **MVC (Modelo-Vista-Controlador)** y el patrón de diseño **DAO**:

* **Lenguaje:** Java (JDK 17 o superior)
* **Interfaz Gráfica:** JavaFX (Diseño SPA estructurado en `.fxml` y estilizado con CSS)
* **Gestor de Dependencias:** Maven
* **Concurrencia:** Uso de la clase `Timeline` de JavaFX y `Platform.runLater` para la sincronización de temporizadores asíncronos sin bloquear la UI.
* **Base de Datos:** MongoDB Atlas (NoSQL en la nube) utilizando *MongoDB Sync Driver*.

## ✨ Características Principales

* **Sistema de Autenticación en la Nube:** Login y registro de usuarios persistente en clúster MongoDB.
* **Motor Pomodoro Seguro:** Sistema de buffer de memoria que recompensa por bloques completos de 60 segundos reales estudiados, evitando hiperinflación.
* **Invernadero Virtual:** Cuadrícula dinámica de 3x3 donde plantar semillas, regarlas y verlas crecer (Fase 0 a Fase 2) o marchitarse (Fase 3).
* **Tienda y Economía Pasiva:** Generación de ingresos pasivos calculados mediante la clase `ChronoUnit` y compra de herramientas (Fertilizantes, Toldos, Palas).
* **Panel de Estadísticas:** Generación de analíticas de rendimiento (`BarChart` y `PieChart`) y cálculo de Rango de Ligas en base a la experiencia obtenida.

## 🚀 Instalación y Uso

Para ejecutar el proyecto en tu entorno local:

1. Clona este repositorio abriendo tu terminal y ejecutando:

```bash
git clone [https://github.com/ousamaka/studybuddy.git](https://github.com/ousamaka/studybuddy.git)
