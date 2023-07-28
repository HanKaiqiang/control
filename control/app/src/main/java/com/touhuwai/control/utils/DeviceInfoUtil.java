package com.touhuwai.control.utils;

import static android.content.Context.WIFI_SERVICE;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

public class DeviceInfoUtil {

    /**
     * 获取设备唯一标识符
     */
    public static String getDeviceId(Context context) {
        String imei = "";
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//            if (tm != null) {
//                imei = tm.getDeviceId();
//            }
        }

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        if(TextUtils.isEmpty(imei) && TextUtils.isEmpty(androidId)){
            return "";
        } else if(!TextUtils.isEmpty(imei) && !TextUtils.isEmpty(androidId)){
            return imei + "_" + androidId;
        } else {
            return !TextUtils.isEmpty(imei) ? imei : androidId;
        }
    }
    public static int getRssi (Context context) {
        WifiManager wifi_service = (WifiManager)context.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifi_service.getConnectionInfo();
        return wifiInfo.getRssi();
    }

    public static String getDeviceIpAddress(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                int networkType = networkInfo.getType();
                if (networkType == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null) {
                            return intToIpAddress(wifiInfo.getIpAddress());
                        }
                    }
                } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                    try {
                        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                             en.hasMoreElements();) {
                            NetworkInterface networkInterface = en.nextElement();
                            for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
                                 enumIpAddr.hasMoreElements();) {
                                InetAddress inetAddress = enumIpAddr.nextElement();
                                if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                    return inetAddress.getHostAddress().toString();
                                }
                            }
                        }
                    } catch (SocketException ignored) {
                    }
                }
            }
        }
        return "";
    }

    private static String intToIpAddress(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                ((ipAddress >> 24) & 0xFF);
    }

    /**
     * 获取设备型号
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * 获取系统版本号
     */
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取语言
     */
    public static String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取微信版本号
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public static String getWechatVersion(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String versionName = packageManager.getPackageInfo("com.tencent.mm", 0).versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
