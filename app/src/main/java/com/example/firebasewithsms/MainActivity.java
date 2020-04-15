package com.example.firebasewithsms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    TextView tv, tvnumero;
    Button btn; //
    EditText et;
    String defaultNumberToSMS = "1234567890";
    HashMap<String, String> globalLastSMS;
    Boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.textView);
        tvnumero = (TextView) findViewById(R.id.textView2);
        btn = (Button) findViewById(R.id.button);
        et = (EditText) findViewById(R.id.editText);
        et.setHint("SMS (default " + defaultNumberToSMS + ")");
        tvnumero.setText("numero para envio SMS: " + defaultNumberToSMS);
        globalLastSMS = getLastMessage();

        watchFirebase("message");
        watchSMS();

    }

    public void sendSMS(String message) {
        SmsManager.getDefault().sendTextMessage
                (defaultNumberToSMS, null, message, null, null);
    }

    public void sendSMSFunction(View v) {

        sendSMS("Este é um SMS de Teste");
    }

    public void updateNumber(View v) {

        defaultNumberToSMS = et.getText().toString();

        tvnumero.setText(defaultNumberToSMS);
    }

    private HashMap<String, String> getLastMessage() {

        HashMap<String, String> msgData = new HashMap<>();

        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData.put(cursor.getColumnName(idx), cursor.getString(idx));
                    }
                    return msgData;
                } while (cursor.moveToNext());
            }
            else {
                return msgData;
            }
        }
        msgData.put("Error:", "SMS_READ Permissions not granted, please check sms permission");
        return msgData;
    }

    private void insertFirebase(String reference, String value) {
        FirebaseApp.initializeApp(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(reference);
        myRef.setValue(value);
    }

    // processo para ouvir o Firebase (especificando o path que deseja ouvir)

    private void watchFirebase(final String path) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbReference = db.getReference(path);
        dbReference.addValueEventListener(new ValueEventListener() {


            // AQUI VOCE DEFINE A ACAO QUANDO O BANCO FIREBASE FOR ALTERADO
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // first run impede que a aplicacao envie SMS ao iniciar
                // ela primeiro le do último sms e depois verifica as alteracoes no banco para informar

                if (!firstRun){
                    String message = dataSnapshot.getValue(String.class);
                    sendSMS("VALOR ALTERADO: "+message+". responda MAIS para mais detalhes");
                }

                firstRun = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                sendSMS("Alteracao no banco cancelada");
            }
        });
    }

    // tostadeira
    private void toaster(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // processo para ouvir os SMSs
    private void watchSMS (){
        SmsProcess smsProcess = new SmsProcess();
        new TaskExecutor().execute(smsProcess);
    }

    // classe de execucao assincrona para o servico de SMS (recebimento e envio de respostas automaticas)
    // pode ser feita refatoracao para outros .class

    private class SmsProcess implements Runnable{
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {

                HashMap<String, String> lastSMSQuery = getLastMessage();

                // aqui voce pode receber todas as mensagens e realizar o tratamento de cada uma de acordo com o necessario
                // dentro do HashMap de SMS, possui todos os valores necessarios, como timestamp, remetente, destinarario, corpo...

                // aqui so eh um exemplo de um chatbot bem basico mesmo

                if (! globalLastSMS.equals(lastSMSQuery) && lastSMSQuery.get("body").toUpperCase().equals("MAIS")){
                    sendSMS("1 - JSON Completo 2 - Registro Alterado");
                    globalLastSMS = lastSMSQuery;
                }
                if (! globalLastSMS.equals(lastSMSQuery) && lastSMSQuery.get("body").toUpperCase().equals("1")){
                    sendSMS(lastSMSQuery.toString().substring(0,150));
                    globalLastSMS = lastSMSQuery;
                }
                if (! globalLastSMS.equals(lastSMSQuery) && lastSMSQuery.get("body").toUpperCase().equals("1")){
                    sendSMS("VOCE ESCOLHEU A OPCAO DOIS");
                    globalLastSMS = lastSMSQuery;
                }
            }
        }
    }

    // executor para a classe SmsProccess
    private class TaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
}
