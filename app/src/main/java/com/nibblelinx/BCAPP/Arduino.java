package com.nibblelinx.BCAPP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Arduino extends AppCompatActivity {

    // Button ButtonMS9Back;

    Button buttonConectarArduino, buttonLED1, buttonLED2, buttonLED3;
    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;

    private static final int MESSAGE_READ = 3;

    MyBluetoothService.ConnectedThread connectedThread;

    static Handler handler;

    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;
    UUID meu_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino);

        buttonConectarArduino = findViewById(R.id.buttonConectarArduino);
        buttonLED1 = findViewById(R.id.buttonLED1);
        buttonLED2 = findViewById(R.id.buttonLED2);
        buttonLED3 = findViewById(R.id.buttonLED3);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        if(meuBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui bluetooth", Toast.LENGTH_LONG).show();
        }else if(!meuBluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
        }

        buttonConectarArduino.setOnClickListener(v -> {
            if(conexao){
                //desconectar
                try{
                    meuSocket.close();
                    conexao = false;
                    buttonConectarArduino.setText("Conectar");
                    Toast.makeText(getApplicationContext(), "O bluetooth foi desconectado", Toast.LENGTH_LONG).show();

                } catch (IOException erro){
                    Toast.makeText(getApplicationContext(), "ocorreu um erro: " + erro, Toast.LENGTH_LONG).show();

                }
            }else{
                Intent abreLista = new Intent(Arduino.this, com.nibblelinx.BCAPP.ListaDispositivos.class);//conectar
                startActivityForResult(abreLista, SOLICITA_CONEXAO );
            }
        });

        buttonLED1.setOnClickListener(view -> {

            if(conexao) {
                connectedThread.enviar("led1");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();
            }
        });



        buttonLED2.setOnClickListener(view -> {

            if(conexao){
                connectedThread.enviar("led2");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();

            }
        });

        buttonLED3.setOnClickListener(view -> {

            if(conexao){
                connectedThread.enviar("led3");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();

            }
        });

        handler = new Handler(Looper.myLooper()) {

            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;

                    dadosBluetooth.append(recebidos);

                    int fimInformacao = dadosBluetooth.indexOf("}");

                    if(fimInformacao > 0) {

                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);

                        int tamInformacao = dadosCompletos.length();

                        if(dadosBluetooth.charAt(0) ==  '{'){

                            String dadosFinais = dadosBluetooth.substring(1, tamInformacao);

                            Log.d("Recebidos", dadosFinais);
                            Variables.Arduino = dadosFinais;

                            if(dadosFinais.contains("l1on")){
                                buttonLED1.setText("LED 1 LIGADO");
                                Log.d("LED1", "ligado");
                            }else if(dadosFinais.contains("l1off")){
                                buttonLED1.setText("LED 1 DESLIGADO");
                                Log.d("LED1", "desligado");

                            }

                            if(dadosFinais.contains("l2on")){
                                buttonLED2.setText("LED 2 LIGADO");
                                Log.d("LED2", "ligado");
                            }else if(dadosFinais.contains("l2off")){
                                buttonLED2.setText("LED 2 DESLIGADO");
                                Log.d("LED2", "desligado");

                            }


                            if(dadosFinais.contains("l3on")){
                                buttonLED3.setText("LED 3 LIGADO");
                                Log.d("LED3", "ligado");
                            }else if(dadosFinais.contains("l3off")){
                                buttonLED3.setText("LED 3 DESLIGADO");
                                Log.d("LED3", "desligado");

                            }

                        }
                        dadosBluetooth.delete(0,dadosBluetooth.length());
                        textFormat(Variables.Arduino);

                    }
                }
            }
        };



    }



    public void textFormat(String text){

        int numberOfNonChar = 0;

        for (int i = 0; i < text.length(); i++)
            if (text.charAt(i) > 0xFF) numberOfNonChar++;

        byte[] newTextChar = new byte[text.length() + numberOfNonChar];

        for (int i = 0, j = 0; i < text.length(); i++, j++) {
            if (text.charAt(i) > 0xFF) {
                newTextChar[j] = (byte) ((text.charAt(i) / 0x100) & 0xFF);
                j++;
                newTextChar[j] = (byte) (text.charAt(i) & 0xFF);
            } else {
                newTextChar[j] = (byte) (text.charAt(i) & 0xFF);
            }
        }
        sendTX(newTextChar);
    }

    public void sendTX(byte[] newTextChar)
    {

        // String pvtkey = Variables.MainPaymail;
        String pvtkey = "9567767df8b9bd7e2b003b25db22e63f013f8bf8f53299890e3a811f60bad57b";
        //String sendTo = SendTo.getText().toString();
        //String sats = Satoshis.getText().toString();
        String data = SHA256G.ByteToStrHex(newTextChar);


        //////////////////////////////////////////////////////////////////////////////////////////////////
        //Preparação das Chaves
        //////////////////////////////////////////////////////////////////////////////////////////////////
        Keygen pubKey = new Keygen();
        //Boolean CompPKey = false;
        //Variables.CompPKey = false;

        Variables.CompPKey = true;

        String PUBKEY = pubKey.publicKeyHEX(pvtkey); //PVTKEY - string Hexadecimal de 64 elementos.
        String BSV160 = pubKey.bsvWalletRMD160(PUBKEY, Variables.CompPKey);
        String BSVADD = pubKey.bsvWalletFull(PUBKEY, Variables.CompPKey);


        /////////////////////////////////////////////////////////////////////
        //User Data Input
        /////////////////////////////////////////////////////////////////////

        String [] PayWallets = new String[10];
        String [] PayValues = new String[10];
        String [] OP_RETURNs = new String[10];

        //PayWallets[0] = "1B69q3ZY6VsuKwCinvbB5tkKWLjHWfGz1J"; //MoneyButton
        //PayWallets[0] = sendTo; //Carteira para onde esta sendo enviado
        //PayWallets[1] = BSVADD;
        PayWallets[0] = BSVADD;
        PayValues[0] = "0";
        //PayValues[0] = "1000";
        //PayValues[0] = sats;
        //...at the name of Jesus every knee should bow, of things in heaven, and things in earth, and things under the earth;
        //OP_RETURNs[0] = "2e2e2e617420746865206e616d65206f66204a65737573206576657279206b6e65652073686f756c6420626f772c206f66207468696e677320696e2068656176656e2c20616e64207468696e677320696e2065617274682c20616e64207468696e677320756e646572207468652065617274683b";

        int nOR = 0;
        if(data.length() > 0) {
            //OP_RETURNs[0] = StrToHex(data);
            OP_RETURNs[0] = SHA256G.ByteToStrHex(newTextChar);
            //OP_RETURNs[0] = "5465737465204e205454542074" + "5465737465204e205454542074";
            nOR = 1;
        }

        if(nOR == 0) {

            Toast.makeText(Arduino.this, "No Data!!!"
                    , Toast.LENGTH_LONG).show();
            return;
        }

        /////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////

        BsvTxCreation txCreate = new BsvTxCreation();

        //Problema Aqui;
        String newTX = txCreate.txBuilder(pvtkey, Variables.CompPKey,1 + nOR, PayWallets,PayValues,OP_RETURNs, nOR);
        String result = "";
        //if(newTX.compareTo("Error 1")==0 || newTX.compareTo("Error 2")==0)

        if(newTX.length()>5)
            if(newTX.substring(0,5).compareTo("Error")==0)
                result = newTX;


        //if(newTX.substring(0,5).compareTo("Error")==0)
        //    result = newTX;
        //else
        {

            Variables.LastTxHexData = newTX;

            BsvTxOperations bsvTxOp = new BsvTxOperations();
            bsvTxOp.txID(newTX);
            Variables.LastTXID = bsvTxOp.TXID;


            result = txCreate.txBroadCast(newTX);
            //result = "Debug";
        }

        //result = txCreate.totalUnspent(BSVADD);

        //Toast.makeText(NFTText.this, "Result: " + OP_RETURNs[0]
        //Toast.makeText(NFTText.this, "Result: " + result + 1 + nOR
        Toast.makeText(Arduino.this, "Result: " + result
                , Toast.LENGTH_LONG).show();

        //((EditText) findViewById(R.id.ET_TEXTOST)).setText(result);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {

            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(), "O bluetooth foi ativado", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "O bluetooth não foi ativado, o aplicativo será encerrado", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){
                    assert data != null;
                    String MAC = data.getExtras().getString(com.nibblelinx.BCAPP.ListaDispositivos.ENDERECO_MAC);
                    Toast.makeText(getApplicationContext(), "MAC Final: " + MAC, Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);

                    try {
                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(meu_UUID);
                        meuSocket.connect();
                        conexao = true;

                        connectedThread = new MyBluetoothService.ConnectedThread(meuSocket);
                        connectedThread.start();

                        buttonConectarArduino.setText("Desconectar");
                        Toast.makeText(getApplicationContext(), "Você foi conectado com: " + MAC, Toast.LENGTH_LONG).show();
                    } catch (IOException erro) {
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro: " + MAC, Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "falha ao obter o MAC", Toast.LENGTH_LONG).show();

                }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public static class MyBluetoothService {
        private static final String TAG = "MY_APP_DEBUG_TAG";

        // Defines several constants used when transmitting messages between the
        // service and the UI.


        private static class ConnectedThread extends Thread {
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket) {
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                // mmBuffer store for the stream
                byte[] mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);

                        String dadosBt = new String(mmBuffer,0, numBytes);

                        // Send the obtained bytes to the UI activity.
                        handler.obtainMessage(MESSAGE_READ, numBytes, -1, dadosBt).sendToTarget();

                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }


            public void enviar(String dadosEnviar) {
                byte[] msgBuffer = dadosEnviar.getBytes();
                try {
                    mmOutStream.write(msgBuffer);

                } catch (IOException ignored) {

                }
            }

        }
    }
}