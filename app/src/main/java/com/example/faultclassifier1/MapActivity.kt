package com.example.faultclassifier1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var latitude = 22.5726
    private var longitude = 88.3639
    private var faultName = "Fault Location"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        latitude = intent.getDoubleExtra("LATITUDE", 22.5726)
        longitude = intent.getDoubleExtra("LONGITUDE", 88.3639)
        faultName = intent.getStringExtra("FAULT_NAME") ?: "Fault Location"

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        val point = LatLng(latitude, longitude)

        googleMap.addMarker(
            MarkerOptions()
                .position(point)
                .title(faultName)
        )

        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(point, 15f)
        )
    }
}