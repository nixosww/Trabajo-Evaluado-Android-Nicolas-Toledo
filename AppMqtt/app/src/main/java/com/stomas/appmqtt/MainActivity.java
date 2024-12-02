package com.stomas.appmqtt;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
//Librerias Mqtt y formulario
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//librerias firebase
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //Variables de la conexion a MQTT
    private static String mqttHost = "tcp://androidesnix.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "Mensaje";
    private static String User = "androidesnix";
    private static String Pass = "0hG7ERXjsnpPmSnH";

    //Variable que se utilizara para imprimir los datos del sensor
    private TextView textView;
    private EditText editTextMessage;
    private EditText editTextNombre;
    private EditText editTextApellido;
    private EditText editTextNumero;
    private Button botonEnvio;

    //Libreria MQTT
    private MqttClient mqttClient;

    //variables firebase

    private EditText txtMensaje, txtNombre, txtApellido, txtNumero;
    private ListView lista;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Firebase
        CargarListaFirestore();
        db = FirebaseFirestore.getInstance();
        txtMensaje = findViewById(R.id.txtMensaje);
        txtNombre = findViewById(R.id.txtNombre);
        txtApellido = findViewById(R.id.txtApellido);
        txtNumero = findViewById(R.id.txtNumero);
        lista = findViewById(R.id.lista);
        //MQTT

        //Enlace de la variable del id que esta en el activity main donde imprimiremos los datos
        editTextMessage = findViewById(R.id.txtMensaje);
        editTextNombre = findViewById(R.id.txtNombre);
        editTextApellido = findViewById(R.id.txtApellido);
        editTextNumero = findViewById(R.id.txtNumero);
        botonEnvio = findViewById(R.id.enviarDatosFirestore);
        try {
            //Creacion de un cliente mqtt
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            //conexion al servidor mqtt
            mqttClient.connect(options);
            //si se conecta impimira un mensaje de mqtt
            Toast.makeText(this, "Aplicacion conectada al servidor MQTT", Toast.LENGTH_SHORT).show();
            //Manejo de entrega de datos y perdida de conexion
            mqttClient.setCallback(new MqttCallback() {
                //metodo en caso de que la conexion se pierda
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexion perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());

                }

                //Metodo para verificar si el envio fue exitoso
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega Completa");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

        //al dar click en el button enviara el mensaje del topico
        botonEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //obtener el mensaje ingresado por el usuario
                String mensaje = editTextMessage.getText().toString();
                try {
                    //verifico si la conexion mqtt esta activa
                    if (mqttClient != null && mqttClient.isConnected()) {
                        //publicar el mensaje en el topico especificado
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        //mostrar el mensaje enviado en el textview
                        Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error: No se pudo enviar el mensaje. La conexion MQTT no esta activa", Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //Firebase

    public void enviarDatosFirestore(View view){
        String mensaje = txtMensaje.getText().toString();
        String nombre = txtNombre.getText().toString();
        String apellido = txtApellido.getText().toString();
        String numero = txtNumero.getText().toString();
        Map<String, Object> cliente = new HashMap<>();
        cliente.put("rut", mensaje);
        cliente.put("nombre", nombre);
        cliente.put("apellido", apellido);
        cliente.put("numero", numero);

        db.collection("cliente")
                .document(mensaje)
                .set(cliente)
                .addOnSuccessListener(aVoid->{
                    Toast.makeText(MainActivity.this, "Datos enviados a firestore correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e->{
                    Toast.makeText(MainActivity.this, "Error al enviar datos a firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    public void CargarLista(View view){
        CargarListaFirestore();
    }
    public void CargarListaFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cliente")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> listacliente = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String linea = "|| " + document.getString("mensaje") + " || " +
                                        document.getString("nombre") + " || " +
                                        document.getString("apellido") + " || " +
                                        document.getString("numero");
                                        listacliente.add(linea);
                            }
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listacliente
                            );
                            lista.setAdapter(adaptador);
                        } else {
                            Log.e("TAG", "Error obteniendo datos de firestore", task.getException());
                        }
                    }
                });


    }
}
