package de.c3ma.android.roomstate.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import de.c3ma.android.roomstate.R;
import de.c3ma.android.roomstate.RoomWidget;

/**
 * created at 25.07.2010 - 14:55:24<br />
 * creator: ollo<br />
 * project: C3MA_RootState<br />
 * $Id: $<br />
 * @author ollo<br />
 */
public class Updater extends Service {

    private static final String XML_AUSSEN = "aussen";

	private static final String XML_INNEN = "innen";

	private static final String OPEN = "OPEN";

	public static final String EXTRA_WIDGET_COMPONENTNAME = "extra_widget_componentname";
    
    private String C3MA_ROOM_URL = "http://www.ccc-mannheim.de/roomstate/state.xml";
    
    private static final String XML_ROOM_STATE = "status";
    private static final String XML_ROOM_TEMP = "temperatur";
    private static final String XML_ROOM_TIME = "timestamp";
    private static final String XML_ROOM_DOOR = "door";
    
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    
    /**
     * The time in minutes, after the information is discarded.
     */
    public static final long TIME_DIFFERENCE = 30;

    @Override
    public IBinder onBind(Intent intent) {
        // We don't need to bind to this service
        return null;
    }
    
    @Override
    public void onStart(final Intent pIntent, final int pStartId) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        
        /* Retrieve all widgetIDs that were build from this class. */
        final ComponentName specificWidget = pIntent.getParcelableExtra(EXTRA_WIDGET_COMPONENTNAME);
        
        int[] ids = widgetManager.getAppWidgetIds(specificWidget);
        for(int i : ids) {
            final RemoteViews updateView = buildUpdate(this, i);
            widgetManager.updateAppWidget(i, updateView);
        }
    }
    
    /**
     * Build a widget update to show the current Weather using the GoogleWeatherAPI.
     * @param pWidgetID 
     */
    public RemoteViews buildUpdate(final Context ctx, final int pWidgetID) {
        
        RemoteViews updateViews = new RemoteViews(ctx.getPackageName(), R.layout.widget_main);
        
        Intent i=new Intent(this, RoomWidget.class);
        i.setAction(RoomWidget.REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0 , i, 0);
        updateViews.setOnClickPendingIntent(R.id.btnRefresh, pi);
        
        HttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(C3MA_ROOM_URL);
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody=client.execute(getMethod, responseHandler);
            extractXMLdata(responseBody, updateViews);            
            Log.d("httpPost response", responseBody);
        }catch (Exception ex) {
            if (ex.getMessage() != null)
                Log.d("httpPost exception", ex.getMessage());
            ex.printStackTrace();
            updateViews.setTextViewText(R.id.widget_textviewRoomstate, "Exception / Offline?");
            updateViews.setTextColor(R.id.widget_textviewRoomstate, Color.BLUE);
        }
                
        return updateViews;
    }

    /**
     * Parse the following XML-Data:
     * <pre>
     * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     * <raum>
     *  <status>1</status>
     *  <temperatur>
     *     <innen>23.7</innen>
     *     <aussen>17.7</aussen>
     *  </temperatur>
     * </raum>
     * </pre>
     * 
     * @param responseBody the response containing the XML-Data
     * @param updateViews   connection to the UI, that should be updated
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void extractXMLdata(final String responseBody, final RemoteViews updateViews) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(responseBody.getBytes());
        Document dom = builder.parse(is);
        Element root = dom.getDocumentElement();
        NodeList items = root.getElementsByTagName("*");
        
        Resources res = getResources();
        long diff = 0;
        
        for (int i=0;i<items.getLength();i++){
            Node n = items.item(i);
            if (n.getNodeName().equalsIgnoreCase(XML_ROOM_STATE)){
                if (n.getFirstChild().getNodeValue().equals(OPEN)) {
                    updateViews.setTextViewText(R.id.widget_textviewRoomstate, getText(R.string.widget_state_open));
                    updateViews.setTextColor(R.id.widget_textviewRoomstate,
                            res.getColor(R.color.widget_color_open));
                } else {
                    updateViews.setTextViewText(R.id.widget_textviewRoomstate, getText(R.string.widget_state_closed));
                    updateViews.setTextColor(R.id.widget_textviewRoomstate, 
                            res.getColor(R.color.widget_color_closed));
                }
            } else if (n.getNodeName().equalsIgnoreCase(XML_ROOM_TEMP)){
                NodeList innen = n.getChildNodes();
                for (int j=0;j<innen.getLength();j++){
                    Node n1 = items.item(j);
                    if (n1.getNodeName().equals(XML_INNEN))
                        updateViews.setTextViewText(R.id.widget_textTempInside, n1.getFirstChild().getNodeValue() + this.getText(R.string.widget_state_temp));
                    else if (n1.getNodeName().equals(XML_AUSSEN))
                        updateViews.setTextViewText(R.id.widget_textTempOutside, n1.getFirstChild().getNodeValue() + this.getText(R.string.widget_state_temp));
                }
            } else if (n.getNodeName().equalsIgnoreCase(XML_ROOM_TIME)){
            	String servertimestamp = n.getFirstChild().getNodeValue();
            	Date servertime = new Date((Long.parseLong(servertimestamp))*1000);
            	// Get msec from each, and subtract.
                diff = System.currentTimeMillis() - servertime.getTime();
            	/* convert milliseconds to minutes */
                diff = diff / (1000 * 60);
                
            	Log.d("C3MA","Servertime " + servertimestamp + " format: " + format.format(servertime) + " ... Date " + servertime + " information is " + diff + "minutes old.");
            	updateViews.setTextViewText(R.id.widget_textviewTimestamp, this.getText(R.string.widget_state_updated) + " " + format.format(servertime));    	
            } else if (n.getNodeName().equalsIgnoreCase(XML_ROOM_DOOR)){
            	String doorstate = n.getFirstChild().getNodeValue();
            	int door;
            	if (doorstate.equalsIgnoreCase("1")){
            		door = View.VISIBLE;
            	} else {
            		door = View.INVISIBLE;
            	}
            	Log.d("C3MA","Doorstate " + doorstate);
            	updateViews.setViewVisibility(R.id.widget_key,door);
            }
            
        }
        
        /* Check the time accuracy */
        if (diff > TIME_DIFFERENCE)
        {
            updateViews.setTextViewText(R.id.widget_textviewRoomstate, getText(R.string.widget_state_old));
            updateViews.setTextColor(R.id.widget_textviewRoomstate, res.getColor(R.color.widget_color_old));
            
            updateViews.setViewVisibility(R.id.widget_key, View.GONE);
            updateViews.setViewVisibility(R.id.widget_textTempOutside, View.INVISIBLE);
            updateViews.setViewVisibility(R.id.widget_textTempInside, View.INVISIBLE);
            updateViews.setViewVisibility(R.id.imgInside, View.INVISIBLE);
            updateViews.setViewVisibility(R.id.imgOutside, View.INVISIBLE);
            updateViews.setTextViewText(R.id.widget_textviewTimestamp, this.getText(R.string.widget_state_outdated) + " " + diff + "min");
        }
        else
        {
            updateViews.setViewVisibility(R.id.widget_textTempOutside, View.VISIBLE);
            updateViews.setViewVisibility(R.id.widget_textTempInside, View.VISIBLE);
            updateViews.setViewVisibility(R.id.imgInside, View.VISIBLE);
            updateViews.setViewVisibility(R.id.imgOutside, View.VISIBLE);
        }
    }
}
