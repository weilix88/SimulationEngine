package main.java.win;

import com.sun.jna.Native;

public interface Kernel32 extends W32API {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class, DEFAULT_OPTIONS);
    HANDLE GetCurrentProcess();
    int GetProcessId(HANDLE Process);
}
