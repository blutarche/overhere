package com.eggcellent.overhere;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import activity.MainActivity;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.widget.RelativeLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapsFragment extends Fragment {


    private static View view;

    private static GoogleMap mMap;
    private static Double latitude, longitude;

    private FragmentActivity myContext;
    private List<String> friendsId = new ArrayList<String>();
    private List<String> postsId = new ArrayList<String>();

    private static final String TAG = MapsFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        view = (RelativeLayout) inflater.inflate(R.layout.fragment_maps, container, false);
        // Passing hardcoded values for latitude & longitude. Please change as per your need. This is just used to drop a Marker on the Map
        latitude = 26.78;
        longitude = 72.56;

        setUpMapIfNeeded();

        return view;
    }

    public void setUpMapIfNeeded() {
        if (mMap != null) {
            setUpMap();
        }
        if (mMap == null) {
            mMap = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
        getFriendsList();
        getFriendsFeed();
    }

    private static void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("My Home").snippet("Home Address"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,
                longitude), 12.0f));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setUpMapIfNeeded();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        myContext = (FragmentActivity) activity;
        super.onAttach(activity);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onMapsFragmentInteraction(Uri uri);
    }

    private void getFriendsList () {
        friendsId = new ArrayList<String>();
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.d(TAG, "getFriendsData onCompleted : response " + response);
                        try {
                            JSONObject jsonObject = response.getJSONObject();
                            Log.d(TAG, "getFriendsData onCompleted : jsonObject " + jsonObject);
                            JSONObject friends = jsonObject.getJSONObject("data");
                            Iterator<?> keys = friends.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                if (friends.get(key) instanceof JSONObject) {
                                    friendsId.add(friends.getString("id"));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getFriendsFeed() {
        Bundle params = new Bundle();
        params.putString("with", "location");
        postsId = new ArrayList<String>();
        for( String userId : friendsId ) {
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/"+userId+"/feed",
                    params,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            try {
                                JSONObject jsonObject = response.getJSONObject();
                                Log.d(TAG, "getFeed onCompleted : jsonObject " + jsonObject);
                                JSONObject posts = jsonObject.getJSONObject("data");
                                Iterator<?> keys = posts.keys();
                                while (keys.hasNext()) {
                                    String key = (String) keys.next();
                                    if (posts.get(key) instanceof JSONObject) {
                                        postsId.add(posts.getString("id"));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            ).executeAsync();
        }
        displayPostsOnMap();
    }

    private void displayPostsOnMap() {
        for (String postId : postsId) {
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/"+postId,
                    null,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            JSONObject jsonObject = response.getJSONObject();
                            Log.d(TAG, "Post : jsonObject " + jsonObject);
                        }
                    }
            ).executeAsync();
            mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("My Home").snippet("Home Address"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,
                    longitude), 12.0f));
        }
    }

}
