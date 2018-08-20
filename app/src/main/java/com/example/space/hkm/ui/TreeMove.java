package com.example.space.hkm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.space.hkm.R;
import com.example.space.hkm.data.Geometry;
import com.example.space.hkm.helpers.Logger;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

public class TreeMove extends TreeDisplay {
    public void onCreate(Bundle savedInstanceState) {
        mapFragmentId = R.id.moveable_marker_map;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tree_move);
        setUpMapIfNeeded();
        showPositionOnMap();
        SegmentedButton buttons = (SegmentedButton) findViewById(R.id.basemap_controls);
        onMapLoad(map -> {
            MapHelper.setUpBasemapControls(buttons, map);
            plotMarker.setDraggable(true);
        });
    }

    public void submitTreeMove(View view) {
        LatLng position = plotMarker.getPosition();
        try {
            Geometry g = plot.getGeometry();
            g.setY(position.latitude);
            g.setX(position.longitude);
            plot.setGeometry(g);
        } catch (JSONException e) {
            Logger.error(e);
        }
        Intent editPlot = new Intent(this, TreeInfoDisplay.class);
        editPlot.putExtra("plot", plot.getData().toString());
        setResult(RESULT_OK, editPlot);
        finish();
    }

    protected void showPositionOnMap() {
        onMapLoad(map -> {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(plotLocation, DEFAULT_TREE_ZOOM_LEVEL));
            if (plotMarker != null) {
                plotMarker.remove();
            }
            plotMarker = map.addMarker(new MarkerOptions().position(plotLocation).title("")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_mapmarker)));
        });
    }
}
