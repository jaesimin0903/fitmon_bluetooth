package com.example.androidbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.androidbluetooth.NET.BluetoothServer
import com.example.androidbluetooth.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT=1
    private var btPermission = false
    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var binding: ActivityMainBinding
///
    private var handler: Handler = Handler()
    private var sbLog = StringBuilder()
    private var btServer: BluetoothServer = BluetoothServer()

    private lateinit var svLogView: ScrollView
    private lateinit var tvLogView: TextView
    private lateinit var etMessage: EditText
  ///
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setContentView(R.layout.activity_main)
        val bleOnOffBtn:ToggleButton = findViewById(R.id.ble_on_off_btn)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(bluetoothAdapter!=null){
            // Device doesn't support Bluetooth
            if(bluetoothAdapter?.isEnabled==false){
                bleOnOffBtn.isChecked = true
            } else{
                bleOnOffBtn.isChecked = false
            }
        }

        bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            bluetoothOnOff()
        }
    }

    fun scanBt(view:View){
        if(bluetoothAdapter == null){

        }else{
            Log.d("1","in else scanBt")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blueToothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,


                        )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blueToothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                )
            }
        }
    }

    private val blueToothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){permissionsMap->
        if(permissionsMap.values.all {it}){
            btPermission=true
            if(bluetoothAdapter?.isEnabled == false){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            }else{
                Log.d("1","permission scanbt")
                scanBT()
            }
        }else{
            Log.d("1","permission denied")
        }
    }

    private  val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
            result: ActivityResult ->
            if(result.resultCode == RESULT_OK){
                scanBT()
                Log.d("1","activity scanbt")
            }else{
                Log.d("1","result not ok")

            }
    }


    private fun scanBT(){
        Log.d("1","in scanBT")
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val builder = AlertDialog.Builder(this@MainActivity)
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.scab_bt,null)
        builder.setCancelable(false)
        builder.setView(dialogView)
        val btlst = dialogView.findViewById<ListView>(R.id.bt_list)
        val dialog = builder.create()

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            || (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                ),
                1
            )
        }
        Log.d("1","in scanBT after permission check")
        val pairedDevices:Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
        val ADAhere:SimpleAdapter
        var data : MutableList<Map<String?,Any?>?>? = null
        data = ArrayList()
        if(pairedDevices.isNotEmpty()){
            Log.d("1","$pairedDevices")
            val datanum1 : MutableMap<String?, Any?> = HashMap()
            datanum1["A"] = ""
            datanum1["B"]= ""
            data.add(datanum1)
            for(device in pairedDevices){
                Log.d("1","${device}")
                val datanum:MutableMap<String?,Any?> = HashMap()
                datanum["A"] = device.name
                Log.d("1","${device.name}")
                datanum["B"] = device.address
                Log.d("1","${device.address}")
                data.add(datanum)
            }
            val fromwhere = arrayOf("A")
            val viewswhere = intArrayOf(R.id.item_name)

            ADAhere = SimpleAdapter(this@MainActivity,data,R.layout.item_list,fromwhere,viewswhere)
            btlst.adapter = ADAhere
            ADAhere.notifyDataSetChanged()
            btlst.onItemClickListener = AdapterView.OnItemClickListener{ adapterView, view, position,l ->
                val string = ADAhere.getItem(position) as HashMap<String,String>
                val deviceName = string["A"]
                binding.deviceName.text = deviceName
                dialog.dismiss()
            }

        } else{
            Log.d("1","device empty")
            val value = "No Devices found"
            Toast.makeText(this,value,Toast.LENGTH_LONG).show()
            return
        }
        dialog.show()

    }

    fun bluetoothOnOff() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter", "Device doesn't support Bluetooth")
        } else {
            if (bluetoothAdapter?.isEnabled == false) {
                // 위치 권한 확인
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // 위치 권한 요청
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_ENABLE_BT
                    )
                } else {
                    // 블루투스 활성화
                    bluetoothAdapter?.enable()
                    val bondedDevices = bluetoothAdapter?.bondedDevices
                    if (bondedDevices != null) {
                        for (device in bondedDevices) {
                            Log.d("BluetoothApp", "Device: ${device.name}, Address: ${device.address}")
                        }
                    } else {
                        Log.d("BluetoothApp", "No bonded devices found")
                    }
                }
            } else {
                // 블루투스 비활성화
                bluetoothAdapter?.disable()
            }
        }
    }

    inner class ClientThread(device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null

        override fun run() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            bluetoothAdapter?.cancelDiscovery()
            try {
                Log.d("1","Try to connect to server..")

                socket?.connect() // 소켓 연결
            } catch (e: Exception) {
                onError(e)

                e.printStackTrace()
                disconnectFromServer()
            }

            if (socket != null) {
                onConnect()

                commThread = CommThread(socket)
                commThread?.start()
            }
        }

        fun stopThread() {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            try {
                // 소켓 생성
                socket = device.createRfcommSocketToServiceRecord(BTConstant.BLUETOOTH_UUID_INSECURE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class AcceptThread : Thread() {
        private var acceptSocket: BluetoothServerSocket? = null
        private var socket: BluetoothSocket? = null

        override fun run() {
            while (true) {
                socket = try {
                    acceptSocket?.accept() // 연결 요청이 오면 소켓 반환
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }

                if (socket != null) {
                    onConnect()

                    commThread = CommThread(socket)
                    commThread?.start()
                    break
                }
            }
        }

        fun stopThread() {
            try {
                acceptSocket?.close()
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            try {
                // 소켓 생성 For Accept
                acceptSocket = btAdapter.listenUsingRfcommWithServiceRecord(
                    "bluetoothTest",
                    BTConstant.BLUETOOTH_UUID_INSECURE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal inner class CommThread(private val socket: BluetoothSocket?): Thread() {

        override fun run() {
            try {
                outputStream = socket?.outputStream
                inputStream = socket?.inputStream
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var len: Int
            val buffer = ByteArray(1024)
            val byteArrayOutputStream = ByteArrayOutputStream()

            while (true) {
                try {
                    // 데이터 수신
                    len = socket?.inputStream?.read(buffer)!!
                    val data = buffer.copyOf(len)
                    byteArrayOutputStream.write(data)

                    socket.inputStream?.available()?.let { available ->

                        if (available == 0) {
                            val dataByteArray = byteArrayOutputStream.toByteArray()
                            val dataString = String(dataByteArray)
                            onReceive(dataString)

                            byteArrayOutputStream.reset()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopThread()
                    accept()
                    break
                }
            }
        }

        fun stopThread() {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendData(msg: String) {
        if (outputStream == null) return

        try {
            // 데이터 송신
            outputStream?.let {
                onSend(msg)

                it.write(msg.toByteArray())
                it.flush()
            }
        } catch (e: Exception) {
            onError(e)
            e.printStackTrace()
            stop()
        }
    }
}