package es.dam.ousama.modelo.dao;

import com.mongodb.client.*;
import es.dam.ousama.modelo.Estudiante;
import org.bson.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UsuarioDAO {
    private static final String URI = "mongodb+srv://ousamakassimi02_db_user:5FhEo8C1iNZVR3xd@cluster0.xct9wvy.mongodb.net/?appName=Cluster0";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usuarios;
    private MongoCollection<Document> sesiones;

    public UsuarioDAO() {
        try {
            mongoClient = MongoClients.create(URI);
            database = mongoClient.getDatabase("studybuddy");
            usuarios = database.getCollection("usuarios");
            sesiones = database.getCollection("sesiones");
        } catch (Exception e) { System.err.println("Error de conexión: " + e.getMessage()); }
    }

    public Estudiante login(String u, String p) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null && user.getString("password").equals(p)) {
            return obtenerDatosUsuario(u);
        }
        return null;
    }

    public Estudiante obtenerDatosUsuario(String u) {
        Document user = usuarios.find(new Document("username", u)).first();
        if (user != null) {
            Estudiante e = new Estudiante(u);
            e.setPuntosCrecimiento(user.getInteger("xp", 0));
            e.setMinutosEstudio(user.getInteger("minEstudio", 25));
            e.setMinutosDescanso(user.getInteger("minDescanso", 5));
            e.setMetaDiariaMinutos(user.getInteger("metaDiaria", 60));
            return e;
        }
        return null;
    }

    public boolean registrar(String username, String password) {
        if (usuarios.find(new Document("username", username)).first() != null) return false;
        Document nuevo = new Document("username", username)
                .append("password", password).append("xp", 0).append("minEstudio", 25)
                .append("minDescanso", 5).append("metaDiaria", 60)
                .append("misAsignaturas", Arrays.asList("Estudio Libre"));
        usuarios.insertOne(nuevo);
        return true;
    }

    public void guardarConfiguracion(String username, int estudio, int descanso, int meta) {
        try {
            Document update = new Document("$set", new Document("minEstudio", estudio)
                    .append("minDescanso", descanso).append("metaDiaria", meta));
            usuarios.updateOne(new Document("username", username), update);
        } catch (Exception e) {}
    }

    public void guardarMisAsignaturas(String username, List<String> lista) {
        try {
            usuarios.updateOne(new Document("username", username), new Document("$set", new Document("misAsignaturas", lista)));
        } catch (Exception e) {}
    }

    public List<String> obtenerMisAsignaturas(String username) {
        Document user = usuarios.find(new Document("username", username)).first();
        if (user != null && user.containsKey("misAsignaturas")) {
            return user.getList("misAsignaturas", String.class);
        }
        return new ArrayList<>(Arrays.asList("Estudio Libre"));
    }

    public void registrarSesion(String user, String asig, int min) {
        Document doc = new Document("username", user).append("asignatura", asig)
                .append("minutos", min).append("fecha", LocalDate.now().toString());
        sesiones.insertOne(doc);
    }

    public List<Document> obtenerSesiones(String user) {
        return sesiones.find(new Document("username", user)).into(new ArrayList<>());
    }

    public void actualizarXP(String u, int xp) {
        usuarios.updateOne(new Document("username", u), new Document("$set", new Document("xp", xp)));
    }

    public void cerrarConexion() { if (mongoClient != null) mongoClient.close(); }
}