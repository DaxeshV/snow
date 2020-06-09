package snow.player.util;

import android.content.Context;
import android.content.res.Resources;

import snow.player.R;

public class ErrorUtil {
    public static final int ERROR_ONLY_WIFI_NETWORK = 1;
    public static final int ERROR_PLAYER_ERROR = 2;
    public static final int ERROR_NETWORK_UNAVAILABLE = 3;

    public static String getErrorMessage(Context context, int errorCode) {
        Resources res = context.getResources();

        switch (errorCode) {
            case ERROR_ONLY_WIFI_NETWORK:
                return res.getString(R.string.snow_error_only_wifi_network);
            case ERROR_PLAYER_ERROR:
                return res.getString(R.string.snow_error_player_error);
            case ERROR_NETWORK_UNAVAILABLE:
                return res.getString(R.string.snow_error_network_unavailable);
            default:
                return res.getString(R.string.snow_error_unknown_error);
        }
    }
}