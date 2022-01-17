package com.example.incubadora;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class Dispositivos extends AppCompatActivity {

    private ListView lv1;

    private BluetoothAdapter bTAdp;//adaptador bluetoooth
    private ArrayAdapter<String> PariedDevices;//donde se va a depositar los nombres y direcciones bluetooth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos);
        lv1 = (ListView)findViewById(R.id.list);//listView de los dispositicos

        VerificarEstadoBT();

        PariedDevices = new ArrayAdapter<>(this, R.layout.nombres);//Hace el adaptador con la lista

        bTAdp=BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> PD = bTAdp.getBondedDevices();//obtiene del Bluetooth los dispositivos vinculados
        if (PD.size()>0){//si hay algo en la lista
            for (BluetoothDevice device: PD){
                PariedDevices.add(device.getName() + "\n" + device.getAddress());//pasa al array de la lista los nombres de los dispositivos
            }
        }
        lv1.setAdapter(PariedDevices);//visualiza la lista de dispositivos vinculados
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {//si hace clic en la lista
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();//pasa esa informacion a string
                String address = info.substring(info.length()-17);//obtiene los ultimos 17 caracteres de la cadena los cuales corresponde a la direccion mac

                Intent i = new Intent(Dispositivos.this, MainActivity.class);
                i.putExtra("device_addres",address);//etiqueta la direccion mac en memoria publica
                startActivity(i);//inicia la actividad principal
            }
        });

    }

    private void VerificarEstadoBT(){
        bTAdp = BluetoothAdapter.getDefaultAdapter();//obtiene la compativilidad del dispositivo bluetooth
        if(bTAdp == null){
            Toast.makeText(this,"El dispositivo no tiene bluetooth",Toast.LENGTH_SHORT).show();
        }
        else if(bTAdp.isEnabled()){
            Log.d("Dispositivos","Bluetooth Activado");
            //Notifica que el bluetooth esta activado en consola como depuracion
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
            //si el bluetooth esta deshabilitado, pide al ususario que lo habilite
        }
    }

}

