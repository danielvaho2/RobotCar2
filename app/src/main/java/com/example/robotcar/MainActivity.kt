package com.example.robotcar

import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var selectedDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private lateinit var listViewDevices: ListView
    private lateinit var buttonConnect: Button
    private lateinit var buttonForward: Button
    private lateinit var buttonStop: Button
    private lateinit var buttonRight: Button
    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()

    private val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
    private val CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listViewDevices = findViewById(R.id.listViewDevices)
        buttonConnect = findViewById(R.id.buttonConnect)
        buttonForward = findViewById(R.id.buttonForward)
        buttonStop = findViewById(R.id.buttonStop)
        buttonRight = findViewById(R.id.buttonRight)


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        loadBondedDevices() // solo emparejados con nombre

        listViewDevices.setOnItemClickListener { _, _, position, _ ->
            selectedDevice = deviceList[position]
            Toast.makeText(this, "Seleccionado: ${deviceNames[position]}", Toast.LENGTH_SHORT).show()
        }

        buttonConnect.setOnClickListener {
            selectedDevice?.let {
                connectToDevice(it)
            } ?: Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
        }

        buttonForward.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> sendCommand("F")  // empieza a avanzar
                android.view.MotionEvent.ACTION_UP -> sendCommand("S")    // se detiene al soltar
            }
            true
        }

        buttonRight.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> sendCommand("R")  // empieza a girar
                android.view.MotionEvent.ACTION_UP -> sendCommand("S")    // se detiene al soltar
            }
            true
            }

// Esto de abajo se supone es que si hago tap avanza y detiene

        //  buttonForward.setOnTouchListener { _, event ->
        //     when (event.action) {
        //      android.view.MotionEvent.ACTION_DOWN -> sendCommand("F")  // empieza a avanzar
        //     android.view.MotionEvent.ACTION_UP,
        //    android.view.MotionEvent.ACTION_CANCEL -> sendCommand("S")  // se detiene al soltar
        //  }
        //  true
       // }

        // buttonRight.setOnTouchListener { _, event ->
        //  when (event.action) {
        //     android.view.MotionEvent.ACTION_DOWN -> sendCommand("R")  // empieza a girar
        //     android.view.MotionEvent.ACTION_UP,
        //      android.view.MotionEvent.ACTION_CANCEL -> sendCommand("S")  // se detiene al soltar
        //  }
        //   true
        // }
        buttonStop.setOnClickListener { sendCommand("S") }
    }



    private fun loadBondedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        deviceList.clear()
        deviceNames.clear()

        pairedDevices?.forEach { device ->
            val name = device.name
            if (!name.isNullOrEmpty()) { // solo con nombre
                deviceList.add(device)
                deviceNames.add(name)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listViewDevices.adapter = adapter
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Conectado a ${selectedDevice?.name}", Toast.LENGTH_SHORT).show()
                }
                gatt.discoverServices() // muy importante para BLE
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Desconectado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)
            if (service != null) {
                characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Characteristic lista, listo para enviar comandos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        if (characteristic == null) {
            Toast.makeText(this, "No conectado o characteristic no lista", Toast.LENGTH_SHORT).show()
            return
        }
        characteristic!!.value = command.toByteArray()
        bluetoothGatt?.writeCharacteristic(characteristic)
        Toast.makeText(this, "Comando enviado: $command", Toast.LENGTH_SHORT).show()
    }
}