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

// Clase encargada de la persistencia centralizada con MongoDB Atlas.
// Gestiona inserciones, actualizaciones y recuperación del estado íntegro del ecosistema.
public class UsuarioDAO {
    // Cadena de conexión al clúster remoto
    private static final String URI = "mongodb+srv://ousamakassimi02_db_user:5FhEo8C1iNZVR3xd@cluster0.xct9wvy.mongodb.net/?appName=Cluster0";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usuarios;
    private MongoCollection<Document> sesiones;

    public UsuarioDAO() {
        try {
            // Establecimiento de conexión con la base de datos y obtención de colecciones
            mongoClient = MongoClients.create(URI);
            database = mongoClient.getDatabase("studybuddy");
            usuarios = database.getCollection("usuarios");
            sesiones = database.getCollection("sesiones");
        } catch (Exception e) {
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    // Valida credenciales contra la base de datos devolviendo el perfil hidratado si es exitoso
    public Estudiante login(String u, String p) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null && user.getString("password").equals(p)) return obtenerDatosUsuario(u);
        return null;
    }

    // Proceso de hidratación de objetos: reconstruye el jardín botánico y el inventario del usuario
    public Estudiante obtenerDatosUsuario(String u) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null) {
            Estudiante e = new Estudiante(u);

            e.setPuntosCrecimiento(user.getInteger("xp", 0));
            e.setMonedasXP(user.getInteger("monedas", 0));
            e.setGotasAgua(user.getInteger("gotasAgua", 10));
            e.setUltimaRecoleccion(user.getString("ultimaRecoleccion") != null ? user.getString("ultimaRecoleccion") : LocalDate.now().toString());
            e.setMinutosEstudio(user.getInteger("minEstudio", 25));
            e.setMinutosDescanso(user.getInteger("minDescanso", 5));
            e.setMetaDiariaMinutos(user.getInteger("metaDiaria", 60));

            // Rescatar las fechas de las misiones diarias para que no se reinicien al cerrar la app
            if (user.containsKey("ultimaFechaMisionMeta")) e.setUltimaFechaMisionMeta(user.getString("ultimaFechaMisionMeta"));
            if (user.containsKey("ultimaFechaMisionRiego")) e.setUltimaFechaMisionRiego(user.getString("ultimaFechaMisionRiego"));

            if (user.containsKey("inventario")) {
                Document docInv = (Document) user.get("inventario"); java.util.Map<String, Integer> miMochila = new HashMap<>();
                for (String key : docInv.keySet()) miMochila.put(key, docInv.getInteger(key)); e.setInventarioSemillas(miMochila);
            }

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

                        // Parseo de Strings a LocalDate (formato admitido por Java)
                        if (d.containsKey("fechaPlantacion")) p.setFechaPlantacion(LocalDate.parse(d.getString("fechaPlantacion")));
                        if (d.containsKey("ultimaVezRegada")) p.setUltimaVezRegada(LocalDate.parse(d.getString("ultimaVezRegada")));
                        if (d.containsKey("finToldoProtector")) p.setFinToldoProtector(LocalDate.parse(d.getString("finToldoProtector")));
                        jardinCargado[i] = p;
                    } else {
                        jardinCargado[i] = new Planta();
                    }
                }
            } else {
                for (int i = 0; i < 9; i++) jardinCargado[i] = new Planta();
            }
            e.setMiJardinNuevo(jardinCargado);

            return e;
        }
        return null;
    }

    // Inicialización estructural segura para nuevos registros
    public boolean registrar(String username, String password) {
        if (usuarios.find(new Document("username", username)).first() != null) return false;

        List<Document> jardinVacio = new ArrayList<>();
        for (int i = 0; i < 9; i++) { Planta p = new Planta(); jardinVacio.add(new Document("tipo", p.getTipo()).append("fase", p.getFase()).append("hidratacion", p.getHidratacion())); }

        Document nuevo = new Document("username", username)
                .append("password", password)
                .append("xp", 0)
                .append("monedas", 0)
                .append("gotasAgua", 10)
                .append("minEstudio", 25)
                .append("minDescanso", 5)
                .append("metaDiaria", 60)
                .append("misAsignaturas", Arrays.asList("Estudio Libre"))
                .append("inventario", new Document())
                .append("miJardinVivo", jardinVacio)
                .append("ultimaRecoleccion", LocalDate.now().toString())
                .append("ultimaFechaMisionMeta", "")
                .append("ultimaFechaMisionRiego", "");

        usuarios.insertOne(nuevo); return true;
    }

    // Sube a la nube el estado global del usuario consolidado en un solo documento
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

            // Construimos el objeto de actualización con los datos sensibles
            Document updateObj = new Document("monedas", e.getMonedasXP())
                    .append("xp", e.getPuntosCrecimiento())
                    .append("gotasAgua", e.getGotasAgua())
                    .append("inventario", docInventario)
                    .append("miJardinVivo", listaJardin)
                    .append("ultimaRecoleccion", e.getUltimaRecoleccion());

            // Añadimos el rastreo de misiones para prevenir exploits si existen
            if (e.getUltimaFechaMisionMeta() != null) updateObj.append("ultimaFechaMisionMeta", e.getUltimaFechaMisionMeta());
            if (e.getUltimaFechaMisionRiego() != null) updateObj.append("ultimaFechaMisionRiego", e.getUltimaFechaMisionRiego());

            Document update = new Document("$set", updateObj);
            usuarios.updateOne(new Document("username", e.getNombre()), update);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void guardarConfiguracion(String username, int estudio, int descanso, int meta) { try { Document update = new Document("$set", new Document("minEstudio", estudio).append("minDescanso", descanso).append("metaDiaria", meta)); usuarios.updateOne(new Document("username", username), update); } catch (Exception e) {} }
    public void guardarMisAsignaturas(String username, List<String> lista) { try { usuarios.updateOne(new Document("username", username), new Document("$set", new Document("misAsignaturas", lista))); } catch (Exception e) {} }
    public List<String> obtenerMisAsignaturas(String username) { Document user = usuarios.find(new Document("username", username)).first(); if (user != null && user.containsKey("misAsignaturas")) return user.getList("misAsignaturas", String.class); return new ArrayList<>(Arrays.asList("Estudio Libre")); }

    // Registra en la colección "sesiones" los minutos estudiados hoy para trazar los gráficos
    public void registrarSesion(String user, String asig, int min) { Document doc = new Document("username", user).append("asignatura", asig).append("minutos", min).append("fecha", LocalDate.now().toString()); sesiones.insertOne(doc); }
    public List<Document> obtenerSesiones(String user) { return sesiones.find(new Document("username", user)).into(new ArrayList<>()); }

    public void actualizarXP(String u, int xp) { usuarios.updateOne(new Document("username", u), new Document("$set", new Document("xp", xp))); }

    // Cierre riguroso del driver para liberar recursos de red
    public void cerrarConexion() { if (mongoClient != null) mongoClient.close(); }
}