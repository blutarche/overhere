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
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
//        Log.i("currentLatLng", currentLatLng.toString());
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
//                                    Log.e("TEST Friends Filter", user.getString("name"));
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

    private void getFriendsFeed(final String userId, final String profilePicURL) {
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
                                getPost(post.getString("id"), profilePicURL, userId);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void getPost (final String postId, final String profilePicURL, final String userId) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/"+postId,
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        try {
                            Log.d("TEST updated time", "response = "+response.toString());
                            Log.d("TEST updated time", "postId = "+postId);
                            Log.d("TEST updated time", "userId = "+userId);
                            JSONObject jsonObject = response.getJSONObject();
                            Log.d("TEST updated time", "jsonObject = " + jsonObject.toString());
                            String dateTime = jsonObject.getString("created_time");
                            Log.d("TEST updated time", "dateTime = " + dateTime);
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+0000'");
                            Date date = null;
                            try {
                                date = format.parse(dateTime);
                                System.out.println(date);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            if ((hashtagValue=="" || isHashtagInList(jsonObject.getString("message"))) &&
                                (timeValue==100 || date==null || isTimeWithin(date))) {
                                getPlace(postId, jsonObject.getString("message"), jsonObject.getString("story"), profilePicURL);
                            }
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
//                            Log.e("TEST distance", mostRecentLatLng.toString());
//                            Log.e("TEST distance", currentLatLng.toString());
                            if (distanceBetween(mostRecentLatLng, currentLatLng).floatValue() < distance.floatValue()) {
                                pushMarker(latitude, longitude, story, message, profilePicURL, postId);
                            }
                            setPostCount(count - 1);
                            Log.e("TEST updated time", "count = "+count);
                        } catch (Exception e) {
                            setPostCount(count - 1);
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
    private long timeThreshold = 1000000000;
    public int distanceValue = 100;
    private Float distance = 100000000f;
    public String hashtagValue = "";
    private List<String> hashtags;
    public String friendsValue = "";
    private List<String> friends;
    private LatLng currentLatLng;

    public void onRefreshByFilter() {
        timeConvert(timeValue);
        distanceConvert(distanceValue);
        hashtags = Arrays.asList(hashtagValue.split("\\s*,\\s*"));
        friends = Arrays.asList(friendsValue.split("\\s*,\\s*"));
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        }
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

    private boolean isTimeWithin (Date date) {
        long postTime = date.getTime();
        long currentTime = new Date().getTime();
        Log.d("TEST updated time", "postTime = "+postTime);
        Log.d("TEST updated time", "currentTime = "+currentTime);
        return (currentTime-postTime)/1000 < timeThreshold;
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

//        Log.d("TEST distance", dist.toString());
        return dist;
    }

    private void timeConvert(int t) {
        if (t == 100) {
            timeThreshold = 60*60*24*365;
        } else if (95 <= t && t < 100) {
            timeThreshold = 60*60*24*30*11;
        } else if (90 <= t && t < 95) {
            timeThreshold = 60*60*24*30*10;
        } else if (85 <= t && t < 90) {
            timeThreshold = 60*60*24*30*9;
        } else if (80 <= t && t < 85) {
            timeThreshold = 60*60*24*30*8;
        } else if (75 <= t && t < 80) {
            timeThreshold = 60*60*24*30*7;
        } else if (70 <= t && t < 75) {
            timeThreshold = 60*60*24*30*6;
        } else if (65 <= t && t < 70) {
            timeThreshold = 60*60*24*30*5;
        } else if (60 <= t && t < 65) {
            timeThreshold = 60*60*24*30*4;
        } else if (55 <= t && t < 60) {
            timeThreshold = 60*60*24*30*3;
        } else if (50 <= t && t < 55) {
            timeThreshold = 60*60*24*30*2;
        } else if (45 <= t && t < 50) {
            timeThreshold = 60*60*24*30*1;
        } else if (40 <= t && t < 45) {
            timeThreshold = 60*60*24*21;
        } else if (35 <= t && t < 40) {
            timeThreshold = 60*60*24*14;
        } else if (30 <= t && t < 35) {
            timeThreshold = 60*60*24*7;
        } else if (25 <= t && t < 30) {
            timeThreshold = 60*60*24*3;
        } else if (2 <= t && t < 25) {
            timeThreshold = 60*60*t;
        } else if (t == 1) {
            timeThreshold = 60*60;
        } else if (t == 0) {
            timeThreshold = 60*30;
        }
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
