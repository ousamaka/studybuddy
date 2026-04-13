# 🌿 StudyBuddy Desktop

> **Gamificación y productividad para estudiantes que pasan demasiadas horas frente al PC.**
> Proyecto Intermodular - 2º DAM (Desarrollo de Aplicaciones Multiplataforma).

StudyBuddy es una aplicación de escritorio diseñada para combatir la procrastinación digital. A diferencia de un simple cronómetro, esta herramienta transforma el tiempo de estudio en recursos (Puntos de Crecimiento) para cuidar y evolucionar un ecosistema virtual. Si estudias, tu planta crece; si abandonas la sesión, la planta se marchita.

La idea nace de una necesidad real: mantener el foco cuando el entorno de trabajo (el ordenador) es también la mayor fuente de distracciones.

## 🛠️ Stack Tecnológico

El proyecto está construido aplicando una arquitectura **MVC (Modelo-Vista-Controlador)** pura para separar la lógica de negocio del diseño visual:

* **Lenguaje:** Java (JDK 21)
* **Interfaz Gráfica:** JavaFX (Diseño estructurado en `.fxml` y estilizado con CSS)
* **Gestor de Dependencias:** Maven
* **Concurrencia:** Hilos (`Threads`) nativos de Java y `Platform.runLater` para la actualización de la UI en tiempo real sin cuelgues.
* **Base de Datos:** MySQL (Conexión mediante JDBC y patrón DAO) *[En desarrollo]*

## ✨ Características Principales

* **Sistema de Autenticación:** Login de usuarios validado para mantener sesiones individuales.
* **Temporizador Pomodoro:** Motor de tiempo ejecutado en segundo plano (hilos) para no interferir con la fluidez de la interfaz gráfica.
* **Gamificación en tiempo real:** Conversión de segundos de concentración en puntos de experiencia (XP) visibles instantáneamente en el Dashboard.
* **Diseño Customizado:** Interfaz moderna que huye de los botones nativos grises, utilizando una paleta de colores verdes y tierra acorde a la temática del ecosistema.

## 🚀 Instalación y Uso (Modo Desarrollo)

Actualmente el proyecto está en fase de desarrollo activo. Para ejecutarlo localmente:

1.  Clona este repositorio:
    ```bash
    git clone [https://github.com/ousamaka/studybuddy.git](https://github.com/ousamaka/studybuddy.git)
    ```
2.  Abre el proyecto en tu IDE favorito (recomendado **IntelliJ IDEA**).
3.  Asegúrate de recargar el archivo `pom.xml` para que Maven descargue las dependencias de JavaFX.
4.  Ejecuta la clase `Lanzador.java` (no `Main.java` directamente para evitar problemas de módulos con JavaFX).

## 🚧 Estado del Proyecto

Actualmente el proyecto se encuentra en un **60% de su desarrollo**:
- [x] Análisis, diseño E-R y prototipado.
- [x] Interfaz gráfica completa (Login y Dashboard) en FXML y CSS.
- [x] Lógica de concurrencia (Hilos y temporizador).
- [ ] Implementación de persistencia con base de datos MySQL local.
- [ ] Animaciones de crecimiento del ecosistema virtual.

## 👨‍💻 Autor

**Ousama Kassimi**
Desarrollador en formación | Estudiante de 2º DAM
