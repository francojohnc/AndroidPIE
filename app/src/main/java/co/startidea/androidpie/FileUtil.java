package co.startidea.androidpie;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtil {
    private static final String LIB_FOLDER = "mytest";
    private static final String TAG = "FILE_UTIL_TAG";
    static Boolean bLoadNativeLib = Boolean.valueOf(false);
    private static Context context;
    private static String dstNativeDir;
    private static String nativeProgramAbsolutePath;
    private static String srcNativeDir;

    static class SharedLibFilter implements FilenameFilter {
        SharedLibFilter() {
        }

        public boolean accept(File dir, String name) {
            if (name.toLowerCase().endsWith(".so")) {
                return true;
            }
            return false;
        }
    }

    public static String getDstNativeProgramAbsolutePath() {
        return dstNativeDir + File.separator + getFileName(nativeProgramAbsolutePath);
    }

    public static void initFileUtil(Context appContext) {
        context = appContext;
        try {
            String appFileDir = context.getFilesDir().getAbsolutePath();
            Log.e(TAG, appFileDir);
            dstNativeDir = appFileDir + "/../" + LIB_FOLDER;
            if (!new File(dstNativeDir).exists()) {
                createFolder(dstNativeDir);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() == null ? "getFileDir() failed!" : e.getMessage());
        }
    }

    public static void recursiveCopyFile(String srcFolderPath, String dstFolderPath) {
        for (File file : new File(srcFolderPath).listFiles()) {
            if (file.isDirectory()) {
                String str = "";
                String str2 = "";
                try {
                    str2 = file.getCanonicalPath();
                    str = dstFolderPath + File.separator + getLastLevelDirectoryName(str2);
                    createFolder(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recursiveCopyFile(str2, str);
            } else if (file.isFile()) {
                String filename = file.getName();
                Log.e(TAG, filename);
                try {
                    copyFile(file.getAbsolutePath(), dstFolderPath + File.separator + filename);
                } catch (Exception e2) {
                    Log.e(TAG, e2.getMessage() == null ? "read file failed!" : e2.getMessage());
                }
            }
        }
    }

    public static void copyAllFiles(String srcFolder, String dstFolder) throws IOException {
        recursiveCopyFile(srcFolder, dstFolder);
        try {
            exec("chmod 0777 " + (dstNativeDir + File.separator + getFileName(nativeProgramAbsolutePath)));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() == null ? "Failed to change permission of native exec file!" : e.getMessage());
        }
    }

    public static void copyFile(String inFile, String outFile) throws IOException {
        FileInputStream in = new FileInputStream(inFile);
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        while (true) {
            int read = in.read(buffer);
            if (read != -1) {
                out.write(buffer, 0, read);
            } else {
                in.close();
                out.flush();
                out.close();
                return;
            }
        }
    }

    public static void recursiveDeleteFile(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    recursiveDeleteFile(file);
                    file.delete();
                }
                if (file.isFile()) {
                    try {
                        file.delete();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to delete file " + file.getName());
                    }
                }
            }
        }
    }

    public static void deleteAllFiles(String folderPath) {
        recursiveDeleteFile(new File(folderPath));
    }

    public static void createFolder(String folerName) {
        File folder = new File(folerName);
        if (!folder.exists()) {
            try {
                boolean success = folder.mkdirs();
                Log.e(TAG, folder.getName() + " is created.");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage() == null ? "create folder failed!" : e.getMessage());
            }
        }
    }

    public static boolean checkFileExist(String filename) {
        File f = new File(filename);
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        return true;
    }

    public static void setSrcNativeProgramAbsolutePath(String fileFullName) {
        nativeProgramAbsolutePath = fileFullName;
    }

    public static String getFilePath(String fileFullName) {
        return fileFullName.substring(0, fileFullName.lastIndexOf("/"));
    }

    public static String getFileName(String fileFullName) {
        return fileFullName.substring(fileFullName.lastIndexOf("/") + 1);
    }

    public static String getFileExtension(String fileFullName) {
        return fileFullName.substring(fileFullName.lastIndexOf(".") + 1);
    }

    public static String getLastLevelDirectoryName(String folderAbsolutePath) {
        return folderAbsolutePath.substring(folderAbsolutePath.lastIndexOf("/") + 1);
    }

    public static boolean checkNativeLibLoaded() {
        return bLoadNativeLib.booleanValue();
    }

    public static String loadDependentLibrary() {
        String message = "";
        for (File file : new File(dstNativeDir).listFiles(new SharedLibFilter())) {
            if (file.isFile() && !file.getName().equals(getFileName(nativeProgramAbsolutePath))) {
                try {
                    System.load(file.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load dependent library: " + file.getName());
                }
                message = message + "Dependent library " + file.getName() + " loaded.\n";
            }
        }
        return message;
    }

    public static String copyResultBack() {
        try {
            copyAllFiles(dstNativeDir, srcNativeDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String loadNativeProgramFiles(String nativeProgram) {
        setSrcNativeProgramAbsolutePath(nativeProgram);
        srcNativeDir = getFilePath(nativeProgramAbsolutePath);
        deleteAllFiles(dstNativeDir);
        try {
            copyAllFiles(srcNativeDir, dstNativeDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ("Native program \"" + getFileName(nativeProgram) + "\" is loaded.") + "\n" + loadDependentLibrary();
    }

    private static void exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try {
                process.waitFor();
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while (true) {
                    String str = stdError.readLine();
                    if (str != null) {
                        Log.e("Exec", str);
                    } else {
                        process.getInputStream().close();
                        process.getOutputStream().close();
                        process.getErrorStream().close();
                        return;
                    }
                }
            } catch (InterruptedException e) {
            }
        } catch (IOException e2) {
        }
    }
}
