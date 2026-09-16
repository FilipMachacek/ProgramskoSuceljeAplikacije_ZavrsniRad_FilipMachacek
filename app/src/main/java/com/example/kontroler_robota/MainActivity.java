package com.example.kontroler_robota;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final String MAC = "00:25:09:50:5A:D3";
    private final UUID UID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter; private BluetoothSocket btUticnica; private OutputStream izlazniTok; private InputStream ulazniTok;
    private View layoutPovezivanje, layoutUpravljanje; private EditText unosP, unosI, unosD, unosKpV, unosKiV; private TextView txtStatus, txtKut;
    private RealTimeGraph grafikon;
    private String trenutnaNaredba = ""; private final Handler upravljac = new Handler(Looper.getMainLooper());
    private final Runnable zadatakKretanja = new Runnable() { @Override public void run() { if (!trenutnaNaredba.isEmpty()) { posaljiNaredbu(trenutnaNaredba); upravljac.postDelayed(this, 100); } } };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        layoutPovezivanje = findViewById(R.id.layoutConnect); layoutUpravljanje = findViewById(R.id.layoutControl);
        unosP = findViewById(R.id.editP); unosI = findViewById(R.id.editI); unosD = findViewById(R.id.editD);
        unosKpV = findViewById(R.id.editKpV); unosKiV = findViewById(R.id.editKiV);
        txtStatus = findViewById(R.id.txtStatus); txtKut = findViewById(R.id.txtAngle);
        grafikon = findViewById(R.id.realTimeGraph);
        findViewById(R.id.imgRobot).startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
        SharedPreferences p = getSharedPreferences("RobotPrefs", 0);
        unosP.setText(p.getString("P", "")); unosI.setText(p.getString("I", "")); unosD.setText(p.getString("D", ""));
        unosKpV.setText(p.getString("KV", "")); unosKiV.setText(p.getString("KI", ""));
        btAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        findViewById(R.id.btnConnect).setOnClickListener(v -> { if (provjeriDopustenja()) poveziSe(); });
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendPid).setOnClickListener(v -> posaljiPid());
        findViewById(R.id.btnSendCascade).setOnClickListener(v -> posaljiKaskadu());
        findViewById(R.id.btnStartBalancing).setOnClickListener(v -> { posaljiPid(); posaljiKaskadu(); v.postDelayed(() -> posaljiNaredbu("G\n"), 500); });

        postaviGumbKretanja(R.id.btnForward, "F\n"); postaviGumbKretanja(R.id.btnBackward, "B\n"); postaviGumbKretanja(R.id.btnLeft, "L\n"); postaviGumbKretanja(R.id.btnRight, "R\n");
        findViewById(R.id.btnStop).setOnClickListener(v -> { trenutnaNaredba = ""; upravljac.removeCallbacks(zadatakKretanja); posaljiNaredbu("X\n"); });
        postaviGumbPolozaja(R.id.btnSquat, "H:55\n", "ČUČANJ (55°)"); postaviGumbPolozaja(R.id.btnNormal, "H:65\n", "NORMALAN (65°)"); postaviGumbPolozaja(R.id.btnStand, "H:85\n", "STAJANJE (85°)");
    }
    private void postaviGumbPolozaja(int id, String naredba, String naslov) { findViewById(id).setOnClickListener(v -> { posaljiNaredbu(naredba); txtKut.setText(naslov); }); }
    @SuppressLint("ClickableViewAccessibility") private void postaviGumbKretanja(int id, String naredba) { findViewById(id).setOnTouchListener((v, e) -> {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#444444")));
            trenutnaNaredba = naredba;
            upravljac.post(zadatakKretanja);
        }
        else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            trenutnaNaredba = "";
            upravljac.removeCallbacks(zadatakKretanja);

            // Šaljemo naredbu za prekid gibanja ili rotacije
            if (naredba.equals("F\n") || naredba.equals("B\n")) {
                posaljiNaredbu("S\n");
            } else if (naredba.equals("L\n") || naredba.equals("R\n")) {
                posaljiNaredbu("C\n");
            }
        }
        return true; }); }
    private boolean provjeriDopustenja() { if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != 0) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1); return false; } return true; }

    private void poveziSe() { if (btAdapter == null || !btAdapter.isEnabled()) { Toast.makeText(this, "BT OFF", Toast.LENGTH_SHORT).show(); return; }
        txtStatus.setText("Spajanje..."); new Thread(() -> { try { BluetoothDevice uredaj = null; for (BluetoothDevice d : btAdapter.getBondedDevices()) if (d.getAddress().equalsIgnoreCase(MAC)) uredaj = d;
            if (uredaj == null) { runOnUiThread(() -> txtStatus.setText("NIJE UPAREN")); return; }
            btAdapter.cancelDiscovery(); try { btUticnica = uredaj.createRfcommSocketToServiceRecord(UID); btUticnica.connect(); } catch (Exception e) { btUticnica = uredaj.createInsecureRfcommSocketToServiceRecord(UID); btUticnica.connect(); }
            izlazniTok = btUticnica.getOutputStream(); ulazniTok = btUticnica.getInputStream();
            runOnUiThread(() -> { txtStatus.setText("AKTIVNA"); layoutPovezivanje.setVisibility(View.GONE); layoutUpravljanje.setVisibility(View.VISIBLE); posaljiNaredbu("CONN\n"); slusajPodatke(); });
        } catch (Exception e) { runOnUiThread(() -> txtStatus.setText("GREŠKA: " + e.getMessage())); } }).start(); }

    private void slusajPodatke() { new Thread(() -> {
        BufferedReader citac = new BufferedReader(new InputStreamReader(ulazniTok));
        while (btUticnica != null && btUticnica.isConnected()) { try { String linija = citac.readLine(); if (linija != null && linija.startsWith("D:")) {
            String[] dijelovi = linija.substring(2).split(",");
            if (dijelovi.length == 2) {
                float kut = Float.parseFloat(dijelovi[0]), brzina = Float.parseFloat(dijelovi[1]);
                runOnUiThread(() -> { txtKut.setText("NAGIB: " + kut + "°"); grafikon.addData(kut, brzina); });
            }
        }} catch (Exception e) { break; } } }).start(); }

    private void posaljiPid() { String p = unosP.getText().toString(), i = unosI.getText().toString(), d = unosD.getText().toString();
        if (p.isEmpty() || i.isEmpty() || d.isEmpty()) return; getSharedPreferences("RobotPrefs", 0).edit().putString("P", p).putString("I", i).putString("D", d).apply(); posaljiNaredbu("P:" + p + ",I:" + i + ",D:" + d + "\n"); }
    private void posaljiKaskadu() { String kv = unosKpV.getText().toString(), ki = unosKiV.getText().toString();
        if (kv.isEmpty() || ki.isEmpty()) return; getSharedPreferences("RobotPrefs", 0).edit().putString("KV", kv).putString("KI", ki).apply();
        posaljiNaredbu("KV:" + kv + "\n"); posaljiNaredbu("KI:" + ki + "\n"); }
    private void posaljiNaredbu(String m) { if (izlazniTok != null) try { izlazniTok.write(m.getBytes()); izlazniTok.flush(); } catch (Exception e) { runOnUiThread(() -> { txtStatus.setText("PREKID"); layoutUpravljanje.setVisibility(View.GONE); layoutPovezivanje.setVisibility(View.VISIBLE); }); } }
    @Override protected void onDestroy() { super.onDestroy(); try { if (btUticnica != null) btUticnica.close(); } catch (Exception ignored) {} }
}
