package sg.vinova.customseekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import sg.vinova.customseekbar.seekbar.FullColorRangeSliderView;
import sg.vinova.customseekbar.seekbar.RangeSliderView;

public class MainActivity extends AppCompatActivity {

    private RangeSliderView smallSlider;

    private RangeSliderView largeSlider;
    private FullColorRangeSliderView multiColorSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        smallSlider = (RangeSliderView) findViewById(
                R.id.rsv_small);
        largeSlider = (RangeSliderView) findViewById(
                R.id.rsv_large);
        final RangeSliderView.OnSlideListener listener = new RangeSliderView.OnSlideListener() {
            @Override
            public void onSlide(int index) {
                Toast.makeText(
                        getApplicationContext(),
                        "Hi index: " + index,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        };
        smallSlider.setOnSlideListener(listener);
        largeSlider.setOnSlideListener(listener);
        multiColorSlider = (FullColorRangeSliderView) findViewById(R.id.multiColorSlider);
        multiColorSlider.setInitialIndex(3);
    }

}
