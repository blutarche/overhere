package com.eggcellent.overhere;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import activity.MainActivity;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MapsFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static GoogleMap mMap;

    private FragmentActivity myContext;
    private LatLng mostRecentLatLng;
    private int count = 0;

    private ProgressDialog progress;
    private final String TAG = MapsFragment.class.getSimpleName();
    private HashMap<Marker, String> mHashMap = new HashMap<Marker, String>();

    private TextView title;
    private TextView description;
    private View view;
    private RelativeLayout pdb;
    private String selectedPostId;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Create an instance of GoogleAPIClient.
        super.onCreate(savedInstanceState);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(myContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

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

        currentLatLng = new LatLng(0f, 0f);
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

    public void forceRefreshFeed() {
        mMap.clear();
        mHashMap.clear();
        count = 0;
        refreshFeed();
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
        if (currentLatLng.latitude!=0f && currentLatLng.longitude!=0f) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10.0f));
            if (distance != 100000000f) {
                mMap.addCircle(new CircleOptions()
                        .center(currentLatLng)
                        .radius(distance)
                        .strokeColor(Color.argb(128, 0, 0, 0))
                        .fillColor(Color.argb(60, 197,224,220)));

            }
        }
        else
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

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        Log.i("currentLatLng", currentLatLng.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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
                            if (hashtagValue=="" || isHashtagInList(jsonObject.getString("message")))
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
                            Log.e("TEST distance", mostRecentLatLng.toString());
                            Log.e("TEST distance", currentLatLng.toString());
                            if (distanceBetween(mostRecentLatLng, currentLatLng).floatValue() < distance.floatValue()) {
                                pushMarker(latitude, longitude, story, message, profilePicURL, postId);
                            }
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
    private Float distance = 100000000f;
    public String hashtagValue = "";
    private List<String> hashtags;
    public String friendsValue = "";
    private List<String> friends;
    private LatLng currentLatLng;

    public void onRefreshByFilter() {
        hashtags = Arrays.asList(hashtagValue.split("\\s*,\\s*"));
        friends = Arrays.asList(friendsValue.split("\\s*,\\s*"));
        distanceConvert(distanceValue);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
            currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
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

    private boolean isHashtagInList(String sHashtag) {
        for (String hashtag:hashtags) {
            if (sHashtag.toLowerCase().contains(hashtag.trim().toLowerCase())) return true;
        }
        return false;
    }

    private Float distanceBetween(LatLng la, LatLng lb) {
        if (currentLatLng == null) return 0f;
        double lat_a = la.latitude;
        double lat_b = lb.latitude;
        double lng_a = la.longitude;
        double lng_b = lb.longitude;
        if (lat_b==0f && lng_b==0f) return 0f;
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float distance = (float)earthRadius * (float)c;

        int meterConversion = 1609;
        Float dist = Float.valueOf(distance * meterConversion);

        Log.d("TEST distance", dist.toString());
        return dist;
    }

    private void distanceConvert(int d) {
        if (d == 100) {
            distance = 100000000f;
        } else if (80 <= d && d < 100) {
            int temp = d - 50;
            distance = temp*100f*1000f;
        } else if (30 <= d && d < 80) {
            int temp = d - 29;
            distance = temp*50f*1000f;
        } else {
            distance = (d+1f)*1000f;
        }
    }

}
