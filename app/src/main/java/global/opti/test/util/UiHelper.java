package global.opti.test.util;

import android.content.Context;

public class UiHelper {

    public static int dpToPixel(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
