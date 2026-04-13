package es.dam.ousama.modelo.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import es.dam.ousama.modelo.Estudiante;
import org.bson.Document;

public class UsuarioDAO {

    // URI configurada con tu usuario y contraseña de clúster actual
    private static final String URI = "mongodb+srv://ousamakassimi02_db_user:5FhEo8C1iNZVR3xd@cluster0.xct9wvy.mongodb.net/?appName=Cluster0";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public UsuarioDAO() {
        try {
            mongoClient = MongoClients.create(URI);
            database = mongoClient.getDatabase("studybuddy");
            collection = database.getCollection("usuarios");
        } catch (Exception e) {
            System.err.println("Error al conectar con MongoDB Atlas: " + e.getMessage());
        }
    }

    /**
     * Intenta autenticar al usuario.
     * @return Objeto Estudiante si las credenciales coinciden, null en caso contrario.
     */
    public Estudiante login(String username, String password) {
        Document filtro = new Document("username", username);
        Document encontrado = collection.find(filtro).first();

        if (encontrado != null) {
            if (encontrado.getString("password").equals(password)) {
                Estudiante est = new Estudiante(username);
                est.setPuntosCrecimiento(encontrado.getInteger("xp", 0));
                return est;
            }
        }
        return null;
    }

    /**
     * Registra un nuevo usuario si el nombre no está en uso.
     * @return true si se crea la cuenta, false si el nombre ya existe.
     */
    public boolean registrar(String username, String password) {
        Document filtro = new Document("username", username);
        if (collection.find(filtro).first() != null) {
            return false;
        }

        Document nuevo = new Document("username", username)
                .append("password", password)
                .append("xp", 0);
        collection.insertOne(nuevo);
        return true;
    }

    public void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    // Actualiza los XP de un usuario específico en la nube
    public void actualizarXP(String username, int nuevosXP) {
        try {
            Document filtro = new Document("username", username);
            Document actualizacion = new Document("$set", new Document("xp", nuevosXP));
            collection.updateOne(filtro, actualizacion);
        } catch (Exception e) {
            System.err.println("Error al guardar XP: " + e.getMessage());
        }
    }
}