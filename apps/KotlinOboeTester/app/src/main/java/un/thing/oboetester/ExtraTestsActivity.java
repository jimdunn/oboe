package un.thing.oboetester;

import android.os.Bundle;
import android.view.View;

import un.thing.MainActivity;

public class ExtraTestsActivity extends BaseOboeTesterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra_tests);
    }

    public void onLaunchMainActivity(View view) {
        launchTestActivity(MainActivity.class);
    }

    public void onLaunchExternalTapTest(View view) {
        launchTestThatDoesRecording(ExternalTapToToneActivity.class);
    }

    public void onLaunchPlugLatencyTest(View view) {
        launchTestActivity(TestPlugLatencyActivity.class);
    }

    public void onLaunchErrorCallbackTest(View view) {
        launchTestActivity(TestErrorCallbackActivity.class);
    }

}
