package co.startidea.androidpie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] cmd = {"/data/user/0/co.startidea.androidpie/cache/c_pie_openvpn.armeabi-v7a","--help"};
        runLinuxCmd(cmd);
    }

    //Run a Linux command and return result
    private void runLinuxCmd(String[] argv) {
        LinkedList<String> argvlist = new LinkedList<String>();

        Collections.addAll(argvlist, argv);

        ProcessBuilder pb = new ProcessBuilder(argvlist);
        // Hack O rama
        String lbpath = genLibraryPath(argv, pb);
        Log.e(TAG, "startOpenVPNThreadArgs:" + lbpath);
        pb.environment().put("LD_LIBRARY_PATH", lbpath);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            // Close the output, since we don't need it
            process.getOutputStream().close();
            InputStream in = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (true) {
                String logline = br.readLine();
                Log.e(TAG, "runLinuxCmd: " + logline);
                if (logline == null) return;
            }

        } catch (IOException e) {
            Log.e(TAG, "onCreate: " + e.getMessage());
//            VpnStatus.logException("Error reading from output of OpenVPN process", e);
//            stopProcess();
        }

    }

    private String genLibraryPath(String[] argv, ProcessBuilder pb) {
        String nativeLibraryDirectory = getApplicationInfo().nativeLibraryDir;
        // Hack until I find a good way to get the real library path
        String applibpath = argv[0].replaceFirst("/cache/.*$", "/lib");
        String lbpath = pb.environment().get("LD_LIBRARY_PATH");
        if (lbpath == null)
            lbpath = applibpath;
        else
            lbpath = applibpath + ":" + lbpath;

        if (!applibpath.equals(nativeLibraryDirectory)) {
            lbpath = nativeLibraryDirectory + ":" + lbpath;
        }
        return lbpath;
    }
}
