package com.eggcellent.overhere;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MapsFragment extends Fragment {


    private static GoogleMap mMap;

    private FragmentActivity myContext;
    private LatLng mostRecentLatLng;
    private int count = 0;

    private ProgressDialog progress;
    private final String TAG = MapsFragment.class.getSimpleName();


    private TextView title;
    private TextView description;
    private View view;
    private RelativeLayout pdb;
    private String selectedPostId;

    private HashMap<Marker, String> mHashMap = new HashMap<Marker, String>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        view = (RelativeLayout) inflater.inflate(R.layout.fragment_maps, container, false);
        mMap = null;
        progress = new ProgressDialog(getActivity());

        title = (TextView) view.findViewById(R.id.place_title);
        description = (TextView) view.findViewById(R.id.place_description);

        pdb = (RelativeLayout) view.findViewById(R.id.place_info_box);

        setUpMapIfNeeded();

        return view;
    }

    public void setUpMapIfNeeded() {
        if (mMap != null) {
            setUpMap();
            mMap.clear();
        }
        if (mMap == null) {
            mMap = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
                mMap.clear();
            }
        }
        getFriendsList();

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                title.setText(marker.getTitle());
                description.setText(marker.getSnippet());
                selectedPostId = mHashMap.get(marker);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                if (pdb.getVisibility() == View.GONE) {
                    pdb.setVisibility(View.VISIBLE);
                }
                return true;
            }

        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if (pdb.getVisibility() == View.VISIBLE) {
                    pdb.setVisibility(View.GONE);
                }
            }
        });
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_place);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent facebookPost = getOpenFacebookIntent(myContext, selectedPostId);
                startActivity(facebookPost);
            }
        });
    }

    public static Intent getOpenFacebookIntent(Context context, String postId) {
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
//            Log.e("TEST Open Facebook app", "fb://post/" + postId);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://post/"+postId));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/me"));
        }
    }

    private static void setUpMap() {

        mMap.setMyLocationEnabled(true);
    }

    public void refreshFeed() {
        if (count == 0) {
//            Log.e("TEST", "getting events!");
            spinnerInitiate();
            getFriendsInfo("me");
            getFriendsList();
        }
    }

    private int getPostCount() {
        return count;
    }

    private void setPostCount(int x) {
        count = x;
        if (count==0) {
            rePositionToRecent();
        }
    }

    private void rePositionToRecent() {
        progress.dismiss();
//        Log.d(TAG, "mostRecentLatLng " + mostRecentLatLng);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mostRecentLatLng, 10.0f));
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setUpMapIfNeeded();
        refreshFeed();
        count = 0;
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

    private void spinnerInitiate() {
        progress.setTitle("Loading event");
        progress.setMessage("Surely it will worth your time...");
        progress.show();
        pdb.setVisibility(View.GONE);
    }

    private void getFriendsList () {
        mMap.clear();
        mHashMap.clear();
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject();
//                            Log.d(TAG, "getFriendsData onCompleted : jsonObject " + jsonObject);
                            JSONArray friends = jsonObject.getJSONArray("data");
//                            Log.d(TAG, "getFriendsData onCompleted : friends " + friends);
                            for(int i=0; i<friends.length(); i++){
                                JSONObject user = friends.getJSONObject(i);
                                if (friendsValue=="" || isFriendsNameInList(user.getString("name"))) {
                                    getFriendsInfo(user.getString("id"));
                                    Log.e("TEST Friends Filter", user.getString("name"));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }



    private void getFriendsInfo(final String userId) {
        Bundle params = new Bundle();
        params.putString("redirect","0");
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+userId+"/picture",
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject();
//                            Log.d(TAG, "getInfo onCompleted : jsonObject " + jsonObject);
                            JSONObject pic = jsonObject.getJSONObject("data");
                            String url = pic.getString("url");
                            getFriendsFeed(userId, url);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getFriendsFeed(String userId, final String profilePicURL) {
        Bundle params = new Bundle();
        params.putString("with", "location");
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+userId+"/feed",
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject();
//                            Log.d(TAG, "getFeed onCompleted : jsonObject " + jsonObject);
                            JSONArray posts = jsonObject.getJSONArray("data");
                            for(int i=0; i<posts.length(); i++) {
                                JSONObject post = posts.getJSONObject(i);
                                getPost(post.getString("id"), profilePicURL);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getPost (final String postId, final String profilePicURL) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+postId,
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject();
//                            Log.d(TAG, "Post : jsonObject " + jsonObject);
                            getPlace(postId, jsonObject.getString("message"), jsonObject.getString("story"), profilePicURL);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getPlace (final String postId, final String message, final String story, final String profilePicURL) {
        Bundle params = new Bundle();
        params.putString("fields", "place");
        count++;
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+postId,
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject();
//                            Log.d(TAG, "Place : jsonObject " + jsonObject);
                            JSONObject place = jsonObject.getJSONObject("place");
                            JSONObject location = place.getJSONObject("location");
                            double latitude = Float.parseFloat(location.getString("latitude"));
                            double longitude = Float.parseFloat(location.getString("longitude"));
                            mostRecentLatLng = new LatLng(latitude, longitude);
                            pushMarker(latitude, longitude, story, message, profilePicURL, postId);
                            setPostCount(count - 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void pushMarker (double latitude, double longitude, String title, String snippet, String markerURL, String postId) {
        try {
            MarkerOptions mark = new MarkerOptions();
            mark.position(new LatLng(latitude, longitude));
            mark.title(title);
            mark.snippet(snippet);
            mark.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
            Marker marker = mMap.addMarker(mark);
            mHashMap.put(marker, postId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FILTER !
    public int timeValue = 100;
    public int distanceValue = 100;
    public String hashtagValue = "";
    private List<String> hashtags;
    public String friendsValue = "";
    private List<String> friends;

    public void onRefreshByFilter() {
        hashtags = Arrays.asList(hashtagValue.split("\\s*,\\s*"));
        friends = Arrays.asList(friendsValue.split("\\s*,\\s*"));
        spinnerInitiate();
        getFriendsList();
    }

    public void onRefreshByNonFilter() {
        timeValue = 100;
        distanceValue = 100;
        friendsValue = "";
        hashtagValue = "";
    }

    private boolean isFriendsNameInList(String fName) {
        for (String name:friends) {
            if (fName.toLowerCase().contains(name.trim().toLowerCase())) return true;
        }
        return false;
    }



}
