package es.dam.ousama.modelo.dao;

import com.mongodb.client.*;
import es.dam.ousama.modelo.Estudiante;
import es.dam.ousama.modelo.Planta;
import org.bson.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// Clase para gestionar toda la conexión con MongoDB Atlas
// Aquí hacemos los inserts, updates y recuperamos los datos del usuario
public class UsuarioDAO {
    // Cadena de conexión a mi cluster de Mongo
    private static final String URI = "mongodb+srv://ousamakassimi02_db_user:5FhEo8C1iNZVR3xd@cluster0.xct9wvy.mongodb.net/?appName=Cluster0";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usuarios;
    private MongoCollection<Document> sesiones;

    public UsuarioDAO() {
        try {
            // Nos conectamos a la bd "studybuddy" y pillamos las colecciones que necesitamos
            mongoClient = MongoClients.create(URI);
            database = mongoClient.getDatabase("studybuddy");
            usuarios = database.getCollection("usuarios");
            sesiones = database.getCollection("sesiones");
        } catch (Exception e) {
            // Si esto peta, lo mostramos por consola para enterarnos
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    // Comprueba credenciales. Si todo ok, nos traemos los datos completos del estudiante
    public Estudiante login(String u, String p) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null && user.getString("password").equals(p)) return obtenerDatosUsuario(u);
        return null;
    }

    // Este es el método tocho. Recupera todas las stats, el inventario y reconstruye el jardín
    public Estudiante obtenerDatosUsuario(String u) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null) {
            Estudiante e = new Estudiante(u);

            // Cargamos stats básicas, si falta algo le metemos valores por defecto
            e.setPuntosCrecimiento(user.getInteger("xp", 0));
            e.setMonedasXP(user.getInteger("monedas", 0));
            e.setGotasAgua(user.getInteger("gotasAgua", 10));
            e.setUltimaRecoleccion(user.getString("ultimaRecoleccion") != null ? user.getString("ultimaRecoleccion") : LocalDate.now().toString());
            e.setMinutosEstudio(user.getInteger("minEstudio", 25));
            e.setMinutosDescanso(user.getInteger("minDescanso", 5));
            e.setMetaDiariaMinutos(user.getInteger("metaDiaria", 60));

            // Rescatar la mochila de semillas y boosters
            if (user.containsKey("inventario")) {
                Document docInv = (Document) user.get("inventario"); java.util.Map<String, Integer> miMochila = new HashMap<>();
                for (String key : docInv.keySet()) miMochila.put(key, docInv.getInteger(key)); e.setInventarioSemillas(miMochila);
            }

            // Reconstruir las 9 macetas del jardín convirtiendo los Document de BSON a objetos Planta de Java
            Planta[] jardinCargado = new Planta[9];
            if (user.containsKey("miJardinVivo")) {
                List<Document> docsJardin = user.getList("miJardinVivo", Document.class);
                for (int i = 0; i < 9; i++) {
                    if (i < docsJardin.size()) {
                        Document d = docsJardin.get(i);
                        Planta p = new Planta();
                        p.setTipo(d.getString("tipo"));
                        p.setFase(d.getInteger("fase", -1));
                        p.setHidratacion(d.getInteger("hidratacion", 0));
                        p.setOroTotalGenerado(d.getInteger("oroTotal", 0));
                        p.setAguaTotalGenerada(d.getInteger("aguaTotal", 0));
                        p.setXpTotalGenerado(d.getInteger("xpTotal", 0));

                        // Fechas (hay que parsearlas a LocalDate porque Mongo guarda Strings)
                        if (d.containsKey("fechaPlantacion")) p.setFechaPlantacion(LocalDate.parse(d.getString("fechaPlantacion")));
                        if (d.containsKey("ultimaVezRegada")) p.setUltimaVezRegada(LocalDate.parse(d.getString("ultimaVezRegada")));
                        if (d.containsKey("finToldoProtector")) p.setFinToldoProtector(LocalDate.parse(d.getString("finToldoProtector")));
                        jardinCargado[i] = p;
                    } else {
                        // Por si acaso hay huecos vacíos o corrompidos
                        jardinCargado[i] = new Planta();
                    }
                }
            } else {
                // Si es una cuenta antigua o sin jardín, le metemos 9 macetas vacías
                for (int i = 0; i < 9; i++) jardinCargado[i] = new Planta();
            }
            e.setMiJardinNuevo(jardinCargado);

            return e;
        }
        return null;
    }

    // Crea una cuenta nueva inicializando todo a 0 (excepto el agua que damos 10 de regalo para empezar)
    public boolean registrar(String username, String password) {
        // Evitar usuarios duplicados
        if (usuarios.find(new Document("username", username)).first() != null) return false;

        List<Document> jardinVacio = new ArrayList<>();
        for (int i = 0; i < 9; i++) { Planta p = new Planta(); jardinVacio.add(new Document("tipo", p.getTipo()).append("fase", p.getFase()).append("hidratacion", p.getHidratacion())); }

        Document nuevo = new Document("username", username).append("password", password).append("xp", 0).append("monedas", 0).append("gotasAgua", 10).append("minEstudio", 25).append("minDescanso", 5).append("metaDiaria", 60).append("misAsignaturas", Arrays.asList("Estudio Libre")).append("inventario", new Document()).append("miJardinVivo", jardinVacio).append("ultimaRecoleccion", LocalDate.now().toString());
        usuarios.insertOne(nuevo); return true;
    }

    // Sube a la nube el estado actual de las plantas, oro, agua y mochila
    public void guardarProgresoJardin(Estudiante e) {
        try {
            Document docInventario = new Document(); e.getInventarioSemillas().forEach(docInventario::append);
            List<Document> listaJardin = new ArrayList<>();
            for (Planta p : e.getMiJardinNuevo()) {
                Document d = new Document("tipo", p.getTipo()).append("fase", p.getFase()).append("hidratacion", p.getHidratacion()).append("oroTotal", p.getOroTotalGenerado()).append("aguaTotal", p.getAguaTotalGenerada()).append("xpTotal", p.getXpTotalGenerado());
                if (p.getFechaPlantacion() != null) d.append("fechaPlantacion", p.getFechaPlantacion().toString());
                if (p.getUltimaVezRegada() != null) d.append("ultimaVezRegada", p.getUltimaVezRegada().toString());
                if (p.getFinToldoProtector() != null) d.append("finToldoProtector", p.getFinToldoProtector().toString());
                listaJardin.add(d);
            }

            // Machacamos los datos viejos con los nuevos en la BD
            Document update = new Document("$set", new Document("monedas", e.getMonedasXP()).append("xp", e.getPuntosCrecimiento()).append("gotasAgua", e.getGotasAgua()).append("inventario", docInventario).append("miJardinVivo", listaJardin).append("ultimaRecoleccion", e.getUltimaRecoleccion()));
            usuarios.updateOne(new Document("username", e.getNombre()), update);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // Updates sueltos para configuraciones rápidas sin tener que subir todo el perfil entero
    public void guardarConfiguracion(String username, int estudio, int descanso, int meta) { try { Document update = new Document("$set", new Document("minEstudio", estudio).append("minDescanso", descanso).append("metaDiaria", meta)); usuarios.updateOne(new Document("username", username), update); } catch (Exception e) {} }
    public void guardarMisAsignaturas(String username, List<String> lista) { try { usuarios.updateOne(new Document("username", username), new Document("$set", new Document("misAsignaturas", lista))); } catch (Exception e) {} }
    public List<String> obtenerMisAsignaturas(String username) { Document user = usuarios.find(new Document("username", username)).first(); if (user != null && user.containsKey("misAsignaturas")) return user.getList("misAsignaturas", String.class); return new ArrayList<>(Arrays.asList("Estudio Libre")); }

    // Registra en la colección "sesiones" los minutos estudiados hoy para sacar las gráficas del Dashboard
    public void registrarSesion(String user, String asig, int min) { Document doc = new Document("username", user).append("asignatura", asig).append("minutos", min).append("fecha", LocalDate.now().toString()); sesiones.insertOne(doc); }
    public List<Document> obtenerSesiones(String user) { return sesiones.find(new Document("username", user)).into(new ArrayList<>()); }

    public void actualizarXP(String u, int xp) { usuarios.updateOne(new Document("username", u), new Document("$set", new Document("xp", xp))); }

    // Siempre hay que cerrar la conexión para no dejar hilos pillados colgando
    public void cerrarConexion() { if (mongoClient != null) mongoClient.close(); }
}