package de.c3ma.android.roomstate;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import de.c3ma.android.roomstate.service.Updater;

/**
 * created at 23.07.2010 - 20:44:37<br />
 * creator: ollo<br />
 * project: C3MA_RootState<br />
 * $Id: $<br />
 * 
 * @author ollo<br />
 */
public class RoomWidget extends AppWidgetProvider {

    public static final String REFRESH = "de.c3ma.android.roomstate.refresh";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (REFRESH.equals(intent.getAction())) {
            Log.d("C3MA","Button pressed");
            refreshClassWidgets(ctx);
        } else {
            super.onReceive(ctx, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        refreshClassWidgets(context);
    }

    public static void refreshClassWidgets(final Context pContext) {

        Toast.makeText(pContext,
                pContext.getString(R.string.widget_notification),
                Toast.LENGTH_LONG).show();
        
        /* To prevent any ANR timeouts, we perform the update in a service. */
        final Intent service = new Intent(pContext, Updater.class);
        service.putExtra(Updater.EXTRA_WIDGET_COMPONENTNAME, new ComponentName(pContext, RoomWidget.class));
        pContext.startService(service);
    }
}
