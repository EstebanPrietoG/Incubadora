package com.example.incubadora;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView temperatura, humedad, fase, error;
    EditText referencia;

    Handler btIn;
    final int handlerState = 0;//constante de tipo int
    private BluetoothAdapter btA = null;
    private BluetoothSocket btS = null;
    private StringBuilder DataSIn = new StringBuilder();
    //Los objtos de tipo StringBuldier son parecidos a las variablees de tipo String pero mas optimizadas
    private ConnectedThread MyConexionBT;
    //crea objeto de tipo conected thread
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatura = (TextView) findViewById(R.id.TV_Temp);
        humedad = (TextView) findViewById(R.id.TV_Hum);
        fase = (TextView) findViewById(R.id.TV_ang);
        error= (TextView) findViewById(R.id.TV_Err);
        referencia = (EditText) findViewById(R.id.ED_Ref);

        btIn = new Handler() {//crea un nuevo hilo de ejecucion
            @SuppressLint("SetTextI18n")
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState){//siempre esta reciviendo y mostrando los datos
                    String readMsg = (String) msg.obj;
                    DataSIn.append(readMsg);

                    int endLineIndex = DataSIn.indexOf("\n");
                    int endLineIndex2 = DataSIn.indexOf("\r");

                    if (endLineIndex > 0 || endLineIndex2 > 0){
                        String dataProces = DataSIn.substring(0,endLineIndex);
                        String[] parts = dataProces.split(";");
                        fase.setText("Fase: " + parts[0] + "°");
                        temperatura.setText("Temperatura: " + parts[1] + "°C");
                        error.setText("Error: " + parts[2] + "°C");
                        humedad.setText("Humedad: " + parts[3] + "%");
                        DataSIn.delete(0,DataSIn.length());
                        //parte la cadena y los ordena en los texview
                    }
                }
            }
        };

        btA = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

    }

    public void prob (View view){

        MyConexionBT.write("x\n\r");//envia el comando para probar
    }

    public void enviar (View view) {//envia la referencia que se anota en el number text
        String strAux = referencia.getText().toString();
        float numAux = Float.parseFloat(strAux);

        if (numAux > 50.0){
            Toast.makeText(this,"La temperatura es demaciado alta",Toast.LENGTH_SHORT).show();
        }
        else if(numAux < 20.0){
            Toast.makeText(this,"La temperatura es demaciado baja",Toast.LENGTH_SHORT).show();
        }
        else {
            MyConexionBT.write(strAux + "\r\n");
        }
    }

    public void desc (View view){//boton desconectar
        if (btS!=null){//si hay conexion bluetooth
            try {
                btS.close();//cierra el socket bluetooth
            }
            catch (IOException e){
                Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
            }
        }
        finish();//cierra la activity
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //crea conexiones seguras usando un UUID
    }

    @Override
    public void onResume(){
        super.onResume();

        Intent intent = getIntent();

        // String para la direccion mac
        String address = intent.getStringExtra("device_addres");//obtiene la direccion mac del dispositivo

        BluetoothDevice device = btA.getRemoteDevice(address);//asigna al dispositivo la direccion mac

        try{
            btS = createBluetoothSocket(device);//crea la conexion segura
        }
        catch (IOException e){
            Toast.makeText(this,"La creacion del socket fallo",Toast.LENGTH_SHORT).show();
        }

        try {
            btS.connect();//se conecta con el dispositivo
        }
        catch (IOException e){
            try {
                btS.close();//cierra la conexion bluetooth
            }
            catch (IOException e2){
                Toast.makeText(this,"Error al cerrarel socket",Toast.LENGTH_SHORT).show();
            }
        }
        MyConexionBT = new ConnectedThread(btS);//crea objeto de hilo
        MyConexionBT.start();//inicia el hilo
    }

    @Override
    public void onPause(){
        super.onPause();
        try {
            btS.close();//cierra el socket bluetooth
        }
        catch (IOException e){
            Toast.makeText(this,"Error al cerrar el socket",Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBTState() {

        if(btA==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btA.isEnabled()) {
                Log.d("Dispositivos","Bluetooth Activado");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //crea streams para la conexion
                tmpIn = socket.getInputStream();//obtiene los datos del socket que entran
                tmpOut = socket.getOutputStream();//obtiene los datos del socket que sale
            } catch (IOException e) {
                Toast.makeText(getBaseContext(),"Error al obtener los streams de la coneccion",Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);         //lee lo que hay en buffer
                    String readMessage = new String(buffer, 0, bytes);//hace el string con lo que hay dentro del buffer
                    // envia el mwnsaje al handler como msg
                    btIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();//envia solo un caracter
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        void write(String input) {
            byte[] msgBuffer = input.getBytes();           //convierte los strings en bytes
            try {
                mmOutStream.write(msgBuffer);                //escribe los datos bt que salen
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();// finaliza la activity

            }
        }
    }
}
