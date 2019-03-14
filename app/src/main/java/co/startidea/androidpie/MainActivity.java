package co.startidea.androidpie;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String binaryPath = write(this);
        ///data/user/0/co.startidea.androidpie/cache/main.x86
        Log.e(TAG, "binaryName " + binaryPath);
        String[] agv = {binaryPath};
        runBinary(agv);
    }

    //Run a Linux command and return result
    private void runBinary(String[] argv) {
        ProcessBuilder pb = new ProcessBuilder(argv);
        // Hack O rama
        String lbpath = genLibraryPath(argv, pb);
        Log.e(TAG, "LD_LIBRARY_PATH: " + lbpath);
        //export environment variable
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
                if (logline == null) return;
                Log.e(TAG, "logline: " + logline);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading from output " + e.getMessage());
            //stop process
        }

    }

    /*get the path of .so files*/

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

    private static final String PIE = "main";
    private static final String NO_PIE = "main";

    /*Position Independent Executable*/
    private static String getExecutableName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return PIE;
        }
        return NO_PIE;
    }

    private String write(Context context) {
        String[] abis;
        /*get abi by build */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abis = getSupportedABIsLollipop();
        } else {
            //noinspection deprecation
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
        /*get abi using native api */
        String nativeAPI = NativeUtils.getNativeAPI();
        /* if abi is not same use the from native api */
        if (!nativeAPI.equals(abis[0])) {
            abis = new String[]{nativeAPI};
        }

        for (String abi : abis) {
            File executableFile = new File(context.getCacheDir(), getExecutableName() + "." + abi);
            /* check if exist if not copy from assets to cache */
            if ((executableFile.exists() && executableFile.canExecute()) || copyBinary(context, abi, executableFile)) {
                return executableFile.getPath();
            }
        }
        return null;
    }

    private static boolean copyBinary(Context context, String abi, File executableFile) {
        try {
            InputStream inputStream;
            try {
                inputStream = context.getAssets().open(getExecutableName() + "." + abi);
            } catch (IOException errabi) {
                Log.e(TAG, "Failed getting assets for architecture " + abi);
                return false;
            }


            FileOutputStream outputStream = new FileOutputStream(executableFile);
            /*buf size*/
            byte buf[] = new byte[4096];

            int lenread = inputStream.read(buf);
            while (lenread > 0) {
                outputStream.write(buf, 0, lenread);
                lenread = inputStream.read(buf);
            }
            outputStream.close();

            if (!executableFile.setExecutable(true)) {
                Log.e(TAG, "Failed to make OpenVPN executable");
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String[] getSupportedABIsLollipop() {
        return Build.SUPPORTED_ABIS;
    }
}
