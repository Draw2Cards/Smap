package com.example.smap

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class ClusterMarkerItem(val pos: LatLng) : ClusterItem {
    override fun getPosition(): LatLng {
        return  pos
    }

    override fun getTitle(): String? {
        return ""
    }


    override fun getSnippet(): String {
        return ""
    }


}


