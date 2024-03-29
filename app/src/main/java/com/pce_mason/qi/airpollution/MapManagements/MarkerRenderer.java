package com.pce_mason.qi.airpollution.MapManagements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.pce_mason.qi.airpollution.R;
import com.pce_mason.qi.airpollution.RealTimeAirDataMonitoring.RealTimeDataItem;

public class MarkerRenderer extends DefaultClusterRenderer<RealTimeDataItem> {

    Context context;
    GoogleMap mMap;
    ClusterManager<RealTimeDataItem> mapItemClusterManager;
    String FocusValue;

    private ClusterManager<RealTimeDataItem> mClusterManager;
//    private final IconGenerator mIconGenerator;
//    private final IconGenerator mClusterIconGenerator;
//    private final ImageView mImageView;
//    private final ImageView mClusterImageView;
//    private final int mDimension;

    public MarkerRenderer(Context context, GoogleMap map, ClusterManager<RealTimeDataItem> clusterManager, LayoutInflater layoutInflater, String FocusValue ) {
        super(context, map, clusterManager); 
        this.context = context;
        this.mMap = map;
        this.mapItemClusterManager = clusterManager;
        this.FocusValue = FocusValue;
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mClusterManager.onCameraIdle();
            }
        });

//        mIconGenerator = new IconGenerator(context);
//        mClusterIconGenerator = new IconGenerator(context);
//
//        View clusterMarker = layoutInflater.inflate(R.layout.cluster_marker, null);
//        mClusterIconGenerator.setContentView(clusterMarker);
//        mClusterImageView = (ImageView) clusterMarker.findViewById(R.id.image);
//
//        mImageView = new ImageView(context);
//        mDimension = (int) context.getResources().getDimension(R.dimen.custom_marker_size);
//        mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
//        int padding = (int) context.getResources().getDimension(R.dimen.custom_marker_padding);
//        mImageView.setPadding(padding, padding, padding, padding);
//        mIconGenerator.setContentView(mImageView);
    }

    @Override
    protected void onBeforeClusterItemRendered(RealTimeDataItem realTimeData, MarkerOptions markerOptions) {
        // Draw a single person.
        // Set the info window to show their name.
//        mImageView.setImageResource(R.drawable.ic_maker_circle_good_50dp);
//        Bitmap bitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
//        Bitmap icon = mIconGenerator.makeIcon();
//        Bitmap icon1 = createDrawableFromView(context,mImageView);
//        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon1)).title(realTimeData.wifiMacAddress).anchor(0.5f,0.5f);
        float mapZoomLevel = mMap.getCameraPosition().zoom;
        float mapZoomLevelRate = (float) Math.pow(2,(mapZoomLevel-12))/1.5f*3;
        int MarkerValue = realTimeData.mHighestValue;
        if(FocusValue.equals(context.getString(R.string.real_data_focus_NO2)))
        {
            MarkerValue = Integer.valueOf(realTimeData.no2Aqi);
        }
        else if(FocusValue.equals(context.getString(R.string.real_data_focus_O3)))
        {
            MarkerValue = Integer.valueOf(realTimeData.o3Aqi);
        }
        else if(FocusValue.equals(context.getString(R.string.real_data_focus_SO2)))
        {
            MarkerValue = Integer.valueOf(realTimeData.so2Aqi);
        }
        else if(FocusValue.equals(context.getString(R.string.real_data_focus_CO)))
        {
            MarkerValue = Integer.valueOf(realTimeData.coAqi);
        }
        else if(FocusValue.equals(context.getString(R.string.real_data_focus_PM25)))
        {
            MarkerValue = Integer.valueOf(realTimeData.pm25);
        }
        else if(FocusValue.equals(context.getString(R.string.real_data_focus_AQI)))
        {
            MarkerValue = realTimeData.mHighestValue;
        }
        int MarkerColor = realTimeData.getMarkerImageColor(MarkerValue);;
        mapZoomLevelRate = 1.0f;
        markerOptions.infoWindowAnchor(100,200);
        markerOptions.icon(bitmapDescriptorFromVector(context,R.drawable.marker_circle_50dp, MarkerColor,mapZoomLevelRate, MarkerValue)).title(realTimeData.wifiMacAddress).anchor(0.5f,0.5f);
    }
//    public MarkerOptions markerResize(RealTimeDataItem realTimeData, MarkerOptions markerOptions){
//        float mapZoomLevel = mMap.getCameraPosition().zoom;
//        float mapZoomLevelRate = (float) Math.pow(2,(mapZoomLevel-12))/1.5f*3;
//        markerOptions.icon(bitmapDescriptorFromVector(context,R.drawable.marker_circle_50dp, realTimeData.getMarkerColor(),mapZoomLevelRate))
//                .title(realTimeData.wifiMacAddress).anchor(0.5f,0.5f);
//        return markerOptions;
//    }

//    @Override
//    protected void onBeforeClusterRendered(Cluster<RealTimeDataItem> cluster, MarkerOptions markerOptions) {
//        // Draw multiple people.
//        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
//        List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
//        int width = mDimension;
//        int height = mDimension;
//
//        for (RealTimeDataItem p : cluster.getItems()) {
//            // Draw 4 at most.
//            if (profilePhotos.size() == 4) break;
//            Drawable drawable = context.getResources().getDrawable(R.drawable.ic_maker_circle_good_50dp);
//            drawable.setBounds(0, 0, width, height);
//            profilePhotos.add(drawable);
//        }
//        MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
//        multiDrawable.setBounds(0, 0, width, height);
//
//        mClusterImageView.setImageDrawable(multiDrawable);
//        Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
//        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
//    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId, int markerColor, float zoomRate, int AqiValue) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        int boundsWidth = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
        int boundsHeight = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boundsWidth,boundsHeight,Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        vectorDrawable.setBounds(0,0,boundsWidth,boundsHeight);
        vectorDrawable.setTint(markerColor);
        vectorDrawable.draw(canvas);

        String text = String.valueOf(AqiValue);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text,bitmap.getWidth()/2,bitmap.getHeight()/2+10,paint);

//        //vector asset size is dp.
//        int boundsWidth = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
//        int boundsHeight = (int)(vectorDrawable.getIntrinsicHeight() *zoomRate);
//        //set bounds and color
//        vectorDrawable.setBounds(0, 0, boundsWidth, boundsHeight);
//        vectorDrawable.setTint(markerColor);
//
//        String text = "11";
//        Bitmap bitmap = Bitmap.createBitmap(boundsWidth, boundsHeight, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor(Color.WHITE); // Text color
//        paint.setTextSize(30); // Text size
//        paint.setFakeBoldText(true);
//        paint.setTextAlign(Paint.Align.CENTER);
////        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE); // Text shadow
//        Rect bounds = new Rect();
//        paint.getTextBounds(text, 0, text.length(), bounds);
//        int x = (bitmap.getWidth() - bounds.width())/2; // 10 for padding from right
//        int y = (bitmap.getHeight() - bounds.height())/2;
//        canvas.drawText(text, x, y, paint);
//
//        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


}