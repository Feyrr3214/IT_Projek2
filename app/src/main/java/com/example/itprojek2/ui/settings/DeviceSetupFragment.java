package com.example.itprojek2.ui.settings;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itprojek2.R;
import com.example.itprojek2.databinding.FragmentDeviceSetupBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceSetupFragment extends Fragment {

    private FragmentDeviceSetupBinding binding;

    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String TARGET_DEVICE_NAME = "Tidak Diketahui";
    private String TARGET_MAC = "";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Thread readThread;
    private volatile boolean keepReading = false;

    // UI Pemindaian Modern
    private BottomSheetDialog bottomSheetDialog;
    private BluetoothAdapterList bluetoothAdapterList;
    private List<BluetoothItem> deviceItemList;
    private int indexNewDevicesHeader = -1;
    private List<String> addedMacAddresses;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    appendLog("Izin scan diberikan. Memulai pemindaian...");
                    startBluetoothScan();
                } else {
                    appendLog("Izin lokasi/Bluetooth ditolak! Tidak bisa memindai.");
                    Toast.makeText(getContext(), "Izin diperlukan untuk memindai", Toast.LENGTH_LONG).show();
                }
            });

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompatCheck(Manifest.permission.BLUETOOTH_CONNECT)) {
                    String deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = "Perangkat Tidak Dikenal";
                    }
                    String mac = device.getAddress();

                    if (!addedMacAddresses.contains(mac)) {
                        addedMacAddresses.add(mac);
                        deviceItemList.add(new BluetoothItem(device, deviceName, mac));
                        if (bluetoothAdapterList != null) {
                            bluetoothAdapterList.notifyDataSetChanged();
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                appendLog("Pemindaian selesai.");
                binding.btnConnect.setEnabled(true);
                binding.btnConnect.setText("Pindai Perangkat Bluetooth");
                
                // Matikan icon loading di header
                if (indexNewDevicesHeader != -1 && indexNewDevicesHeader < deviceItemList.size()) {
                    deviceItemList.get(indexNewDevicesHeader).isScanningHeader = false;
                    if (bluetoothAdapterList != null) {
                        bluetoothAdapterList.notifyItemChanged(indexNewDevicesHeader);
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnConnect.setOnClickListener(v -> {
            if (isConnected) disconnectBluetooth();
            else checkPermissionsAndScan();
        });
        binding.btnSendWifi.setOnClickListener(v -> sendWifiCredentials());

        updateUiState();
    }

    private boolean ActivityCompatCheck(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permission.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
            return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkPermissionsAndScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            appendLog("Bluetooth mati. Nyalakan dahulu.");
            Toast.makeText(getContext(), "Nyalakan Bluetooth terlebih dahulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> req = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            req.add(Manifest.permission.BLUETOOTH_SCAN);
            req.add(Manifest.permission.BLUETOOTH_CONNECT);
            req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        boolean allGranted = true;
        for (String p : req) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false; break;
            }
        }

        if (!allGranted) requestPermissionsLauncher.launch(req.toArray(new String[0]));
        else startBluetoothScan();
    }

    private void startBluetoothScan() {
        if (!ActivityCompatCheck(Manifest.permission.BLUETOOTH_SCAN)) return;

        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

        deviceItemList = new ArrayList<>();
        addedMacAddresses = new ArrayList<>();

        // 1. Tersimpan (Paired)
        deviceItemList.add(new BluetoothItem("TERSIMPAN", false));
        if (ActivityCompatCheck(Manifest.permission.BLUETOOTH_CONNECT)) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice dev : pairedDevices) {
                String devName = dev.getName();
                if (devName == null || devName.isEmpty()) devName = "Unknown (Paired)";
                deviceItemList.add(new BluetoothItem(dev, devName, "Tersimpan"));
                addedMacAddresses.add(dev.getAddress());
            }
        }

        // 2. Perangkat Baru
        indexNewDevicesHeader = deviceItemList.size();
        deviceItemList.add(new BluetoothItem("PERANGKAT YANG TERSEDIA", true));

        showBottomSheetScanner();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireActivity().registerReceiver(scanReceiver, filter);

        bluetoothAdapter.startDiscovery();

        binding.btnConnect.setEnabled(false);
        binding.btnConnect.setText("Memindai...");
        appendLog("Membuka Pindai Bluetooth modern...");
    }

    private void showBottomSheetScanner() {
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bluetooth_scan, null);
        bottomSheetDialog.setContentView(dialogView);

        RecyclerView rvDevices = dialogView.findViewById(R.id.rvBluetoothDevices);
        rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        bluetoothAdapterList = new BluetoothAdapterList(deviceItemList, device -> {
            if (bluetoothAdapter.isDiscovering() && ActivityCompatCheck(Manifest.permission.BLUETOOTH_SCAN)) {
                bluetoothAdapter.cancelDiscovery();
            }
            if(bottomSheetDialog != null) bottomSheetDialog.dismiss();
            connectToBluetoothDevice(device);
        });
        rvDevices.setAdapter(bluetoothAdapterList);

        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (bluetoothAdapter.isDiscovering() && ActivityCompatCheck(Manifest.permission.BLUETOOTH_SCAN)) {
                bluetoothAdapter.cancelDiscovery();
            }
            if(!isConnected) {
                binding.btnConnect.setEnabled(true);
                binding.btnConnect.setText("Pindai Perangkat Bluetooth");
            }
        });

        // Set background transparent so custom top-rounding displays correctly
        if(bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet)
                             .setBackgroundResource(android.R.color.transparent);
        }

        bottomSheetDialog.show();
    }

    private void connectToBluetoothDevice(BluetoothDevice espDevice) {
        if (!ActivityCompatCheck(Manifest.permission.BLUETOOTH_CONNECT)) return;

        TARGET_DEVICE_NAME = espDevice.getName() != null ? espDevice.getName() : "Tanpa Nama";
        TARGET_MAC = espDevice.getAddress();

        appendLog("Terpilih: " + TARGET_DEVICE_NAME);
        binding.btnConnect.setText("Menghubungkan...");
        binding.btnConnect.setEnabled(false);

        new Thread(() -> {
            try {
                if (btSocket != null) {
                    try { btSocket.close(); } catch (IOException ignored) {}
                }

                if(!ActivityCompatCheck(Manifest.permission.BLUETOOTH_CONNECT)) return;
                btSocket = espDevice.createRfcommSocketToServiceRecord(BT_UUID);
                btSocket.connect();

                outputStream = btSocket.getOutputStream();
                inputStream = btSocket.getInputStream();
                isConnected = true;
                startReadThread();

                mainHandler.post(() -> {
                    appendLog("✓ Berhasil terhubung ke " + TARGET_DEVICE_NAME + "!");
                    updateUiState();
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    appendLog("Gagal terhubung.");
                    isConnected = false;
                    updateUiState();
                });
            }
        }).start();
    }

    private void disconnectBluetooth() {
        keepReading = false;
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (btSocket != null) btSocket.close();
        } catch (IOException e) {}
        
        outputStream = null; inputStream = null; btSocket = null; isConnected = false;
        appendLog("Koneksi Bluetooth diputus.");
        updateUiState();
    }

    private void startReadThread() {
        keepReading = true;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (keepReading) {
                try {
                    if (inputStream != null && inputStream.available() > 0) {
                        int currentBytes = inputStream.read(buffer);
                        String received = new String(buffer, 0, currentBytes).trim();

                        if (received.contains("WiFi Terhubung")) {
                            appendLogFromThread("ESP32: " + received);
                            appendLogFromThread("✓ ESP32 sudah online lewat WiFi!");
                            mainHandler.postDelayed(() -> {
                                disconnectBluetooth();
                                appendLog("Setup selesai! Jaringan beralih.");
                            }, 3000);
                        } else {
                            appendLogFromThread("ESP32: " + received);
                        }
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    if (keepReading) {
                        mainHandler.post(() -> { isConnected = false; updateUiState(); });
                    }
                    break;
                }
            }
        });
        readThread.start();
    }

    private void sendWifiCredentials() {
        String ssid = binding.etSsid.getText().toString().trim();
        String pass = binding.etPassword.getText().toString().trim();
        if (ssid.isEmpty() || pass.isEmpty()) return;
        if (!isConnected || outputStream == null) return;

        String data = "WIFI:" + ssid + "," + pass + "\n";
        binding.btnSendWifi.setEnabled(false);
        binding.btnSendWifi.setText("Mengirim...");

        new Thread(() -> {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
                appendLogFromThread("✓ Data WiFi terkirim!");
            } catch (IOException e) {
                mainHandler.post(() -> {
                    binding.btnSendWifi.setEnabled(true);
                    binding.btnSendWifi.setText("Kirim ke ESP32");
                });
            }
        }).start();
    }

    private void updateUiState() {
        if (isConnected) {
            binding.tvBluetoothStatus.setText("Terhubung: " + TARGET_DEVICE_NAME);
            binding.tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
            binding.viewStatusDot.setBackgroundResource(R.drawable.shape_icon_circle_green);
            binding.btnConnect.setText("Putuskan Koneksi");
            binding.btnConnect.setEnabled(true);
            binding.layoutStep2.setVisibility(View.VISIBLE);
            binding.btnSendWifi.setEnabled(true);
            binding.btnSendWifi.setText("Kirim ke ESP32");
        } else {
            binding.tvBluetoothStatus.setText("Belum Terhubung");
            binding.tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red));
            binding.viewStatusDot.setBackgroundResource(R.drawable.shape_icon_circle_red);
            binding.btnConnect.setText("Pindai Perangkat Bluetooth");
            binding.btnConnect.setEnabled(true);
            binding.layoutStep2.setVisibility(View.GONE);
        }
    }

    private void appendLog(String message) {
        android.util.Log.d("DeviceSetup", message);
    }

    private void appendLogFromThread(String message) {
        mainHandler.post(() -> appendLog(message));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        keepReading = false;
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering() && ActivityCompatCheck(Manifest.permission.BLUETOOTH_SCAN)) {
                bluetoothAdapter.cancelDiscovery();
            }
            try { requireActivity().unregisterReceiver(scanReceiver); } catch (Exception ignored) {}
        } catch(Exception ignored){}
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (btSocket != null) btSocket.close();
        } catch (IOException ignored) {}
        binding = null;
    }

    // ==========================================
    // RECYCLERVIEW DATA & ADAPTER UTILITIES
    // ==========================================
    private static class BluetoothItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_DEVICE = 1;

        int type;
        String title;
        String subtitle;
        BluetoothDevice device;
        boolean isScanningHeader = false;

        BluetoothItem(String headerTitle, boolean isScanningHeader) {
            this.type = TYPE_HEADER;
            this.title = headerTitle;
            this.isScanningHeader = isScanningHeader;
        }

        BluetoothItem(BluetoothDevice device, String title, String subtitle) {
            this.type = TYPE_DEVICE;
            this.device = device;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    // Dipindah keluar agar tidak conflict modifier (non-static inner class)
    interface OnDeviceClickListener { void onDeviceClick(BluetoothDevice device); }

    private class BluetoothAdapterList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<BluetoothItem> items;
        private final OnDeviceClickListener listener;

        BluetoothAdapterList(List<BluetoothItem> items, OnDeviceClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override public int getItemViewType(int position) { return items.get(position).type; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == BluetoothItem.TYPE_HEADER) {
                return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_header, parent, false));
            } else {
                return new DeviceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            BluetoothItem item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder hvh = (HeaderViewHolder) holder;
                hvh.tvHeaderTitle.setText(item.title);
                hvh.pbScanning.setVisibility(item.isScanningHeader ? View.VISIBLE : View.GONE);
            } else if (holder instanceof DeviceViewHolder) {
                DeviceViewHolder dvh = (DeviceViewHolder) holder;
                dvh.tvDeviceName.setText(item.title);
                dvh.tvDeviceStatus.setText(item.subtitle);
                dvh.itemView.setOnClickListener(v -> listener.onDeviceClick(item.device));
            }
        }

        @Override public int getItemCount() { return items.size(); }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeaderTitle; ProgressBar pbScanning;
            HeaderViewHolder(View v) { super(v); tvHeaderTitle = v.findViewById(R.id.tvHeaderTitle); pbScanning = v.findViewById(R.id.pbScanning); }
        }

        class DeviceViewHolder extends RecyclerView.ViewHolder {
            TextView tvDeviceName, tvDeviceStatus;
            DeviceViewHolder(View v) { super(v); tvDeviceName = v.findViewById(R.id.tvDeviceName); tvDeviceStatus = v.findViewById(R.id.tvDeviceStatus); }
        }
    }
}
