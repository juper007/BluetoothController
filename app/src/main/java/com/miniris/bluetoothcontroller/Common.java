package com.miniris.bluetoothcontroller;

import java.util.UUID;

/**
 * Created by v-mipark on 2/9/2015.
 */
public interface Common {
    public final int REQUEST_BLUETOOTH_ENABLE = 0;
    public final UUID MY_UUID = UUID.fromString("0b3c15dd-063a-4921-9bda-103693a1e26f");
    public final String CONTROLLER_NAME = "Juper Controller";

    public final int MESSAGE_READ = 0;
    public final int MESSAGE_WRITE = 1;
}
