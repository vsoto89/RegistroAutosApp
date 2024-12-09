package com.victor.registroautosapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken

class MainActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()

        val btnSaveCar = findViewById<Button>(R.id.btnSaveCar)
        val btnFetchCars = findViewById<Button>(R.id.btnFetchCars)

        btnSaveCar.setOnClickListener { saveCar() }
        btnFetchCars.setOnClickListener { fetchCars() }

        // Configurar y conectar el cliente MQTT
        setupMqttClient()
    }

    // Configuración del cliente MQTT
    private fun setupMqttClient() {
        val brokerUrl = "tcp://torchbear137.cloud.shiftr.io:1883" // Broker de shiftr.io
        val clientId = MqttClient.generateClientId() // Generar un ID único para el cliente

        try {
            mqttClient = MqttClient(brokerUrl, clientId, null) // Crear un cliente MQTT
            val options = MqttConnectOptions()
            options.userName = "torchbear137" // Nombre de usuario de shiftr.io
            options.password = "IiqN1di54WfW5Zb3".toCharArray() // El token que has proporcionado como contraseña

            // Conectar al broker MQTT
            mqttClient.connect(options)

            // Suscribirse a un tema
            mqttClient.subscribe("mi/tema", 1) // "mi/tema" es el tema que queremos suscribir

            mqttClient.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Acción cuando llegue un mensaje
                    runOnUiThread {
                        // Mostrar el mensaje recibido
                        Toast.makeText(applicationContext, "Mensaje: ${message.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    // Acción cuando se pierde la conexión
                    Toast.makeText(applicationContext, "Conexión perdida", Toast.LENGTH_SHORT).show()
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Acción cuando el mensaje es entregado
                }
            })

        } catch (e: MqttException) {
            e.printStackTrace()
            Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para publicar un mensaje en MQTT
    private fun publishMessage(message: String) {
        try {
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttClient.publish("Auto", mqttMessage) // Publicar el mensaje en el tema
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // Guardar un auto en Firestore y publicar en MQTT
    private fun saveCar() {
        val carName = findViewById<EditText>(R.id.etCarName).text.toString()
        val carModel = findViewById<EditText>(R.id.etCarModel).text.toString()
        val carYear = findViewById<EditText>(R.id.etCarYear).text.toString()

        if (carName.isNotEmpty() && carModel.isNotEmpty() && carYear.isNotEmpty()) {
            val car = hashMapOf(
                "name" to carName,
                "model" to carModel,
                "year" to carYear
            )

            // Guardar el auto en Firestore
            firestore.collection("auto").add(car)
                .addOnSuccessListener {
                    // Publicar el mensaje en MQTT
                    val message = "Nuevo auto registrado: $carName - $carModel - $carYear"
                    publishMessage(message)

                    Toast.makeText(this, "Auto registrado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    // Obtener y mostrar los autos registrados en Firestore
    private fun fetchCars() {
        firestore.collection("auto").get()
            .addOnSuccessListener { querySnapshot ->
                val carList = mutableListOf<String>()
                for (document in querySnapshot) {
                    val name = document.getString("name")
                    val model = document.getString("model")
                    val year = document.getString("year")
                    carList.add("$name - $model - $year")
                }
                val listView = findViewById<ListView>(R.id.lvCars)
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, carList)
                listView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

