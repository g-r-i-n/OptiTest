package global.opti.test.view;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import global.opti.test.BuildConfig;
import global.opti.test.R;
import global.opti.test.util.UiHelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int FOCUS_MODE_IS_OTHER = 0;
    private static final int FOCUS_MODE_IS_FROM = 1;
    private static final int FOCUS_MODE_IS_TO = 2;

    private Button toGoogleMapsButton;
    private Button toWazeMapsButton;
    private ProgressBar progressBar;
    private int focus = FOCUS_MODE_IS_TO;

    private GoogleMap mMap;
    private AutocompleteSupportFragment autocompleteFragmentFrom;
    private AutocompleteSupportFragment autocompleteFragmentTo;

    private LatLng fromPosition;
    private LatLng toPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        toGoogleMapsButton = findViewById(R.id.gmaps);
        toWazeMapsButton = findViewById(R.id.wmaps);
        progressBar = findViewById(R.id.progress);

        initPlaces();
        initOther();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initOther() {
        toGoogleMapsButton.setOnClickListener(view -> {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("www.google.com")
                    .appendPath("maps")
                    .appendPath("dir")
                    .appendPath("")
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("mode", "driving")
                    .appendQueryParameter("origin", fromPosition.latitude + "," + fromPosition.longitude)
                    .appendQueryParameter("destination", toPosition.latitude + "," + toPosition.longitude);
            String url = builder.build().toString();
            Log.d("Directions", url);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }

    /**
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        progressBar.setVisibility(View.GONE);
        mMap = googleMap;
        mMap.setOnMapLongClickListener(latLng -> {
            rememberPosition(focus, latLng);
            getFocusedAutocompleteSupportFragment().setText("" + latLng.latitude + ", " + latLng.longitude);});
    }

    private void initPlaces() {
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        autocompleteFragmentFrom = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.from);
        autocompleteFragmentTo = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.to);
        initPlacesFragment(autocompleteFragmentFrom, FOCUS_MODE_IS_FROM);
        initPlacesFragment(autocompleteFragmentTo, FOCUS_MODE_IS_TO);
    }

    private void initPlacesFragment(AutocompleteSupportFragment fragment, int focusMode) {
//        fragment.getView().findViewById(R.id.places_autocomplete_search_input)
//                .setOnFocusChangeListener(( view, isFocused) -> focus = isFocused ? focusMode : focus);
        fragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        fragment.setOnPlaceSelectedListener(new OptiPlaceSelectionListener(focusMode));
        ((EditText)fragment.getView().findViewById(R.id.places_autocomplete_search_input)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                Log.i(TAG, "Text changed");
                if (readyToComposeRoute()) {
                    composeRoute(fromPosition, toPosition);
                }
            }
        });
    }

    private boolean readyToComposeRoute() {
        TextView from = autocompleteFragmentFrom.getView().findViewById(R.id.places_autocomplete_search_input);
        TextView to = autocompleteFragmentTo.getView().findViewById(R.id.places_autocomplete_search_input);
        return !TextUtils.isEmpty(from.getText().toString()) && !TextUtils.isEmpty(to.getText().toString());
    }

    private void composeRoute(LatLng from, LatLng to) {
        progressBar.setVisibility(View.VISIBLE);
        GoogleDirection.withServerKey(BuildConfig.MAPS_API_KEY)
                .from(from)
                .to(to)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        progressBar.setVisibility(View.GONE);
                        if (direction != null && direction.isOK()) {
                            toGoogleMapsButton.setEnabled(true);
                            routeSucess(direction, from, to);
                        } else {
                            Toast.makeText(MapsActivity.this, R.string.something_wrong, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        t.printStackTrace();
                    }
                });
    }

    private void routeSucess(Direction direction, LatLng sourceLatLng, LatLng destinationLatLng) {
        mMap.clear();
        for (Route route : direction.getRouteList()) {
            PolylineOptions polyoptions = new PolylineOptions();
            polyoptions.color(getResources().getColor(R.color.purple_200, null));
            polyoptions.width(5);
            polyoptions.addAll(route.getOverviewPolyline().getPointList());
            Polyline poly = mMap.addPolyline(polyoptions);
            poly.setClickable(true);
        }
        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
        if (sourceLatLng != null) {
            latLngBuilder.include(sourceLatLng);
        }
        if (destinationLatLng != null) {
            latLngBuilder.include(destinationLatLng);
        }
        try {
            LatLngBounds bounds = latLngBuilder.build();
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, UiHelper.dpToPixel(this, 135));
            mMap.animateCamera(cu);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AutocompleteSupportFragment getFocusedAutocompleteSupportFragment() {
        if (focus == FOCUS_MODE_IS_FROM) {
            return autocompleteFragmentFrom;
        } else if (focus == FOCUS_MODE_IS_TO) {
            return autocompleteFragmentTo;
        } else {
            return null;
        }
    }

    class OptiPlaceSelectionListener implements PlaceSelectionListener {

        private int focus;

        public OptiPlaceSelectionListener(int focus) {
            this.focus = focus;
        }

        @Override
        public void onPlaceSelected(@NonNull Place place) {
            Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            rememberPosition(focus, place.getLatLng());
        }


        @Override
        public void onError(@NonNull Status status) {
            Log.i(TAG, "An error occurred: " + status);
        }
    }

    private void rememberPosition(int f, LatLng latLng) {
        if (f == FOCUS_MODE_IS_FROM) {
            fromPosition = latLng;
        } else {
            toPosition = latLng;
        }
    }
}