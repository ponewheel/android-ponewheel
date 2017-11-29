package net.kwatts.powtools;


import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.github.mikephil.charting.charts.PieChart;

import net.kwatts.powtools.util.Util;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;



@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityInstrumentationTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    //private MainActivity mActivity;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBatteryGrid(){

       //onView(withId(R.id.editText)).perform(typeText(STRING_TO_BE_TYPED), closeSoftKeyboard()); //line 1
       //onView(withText("Say hello!")).perform(click()); //line 2

        final PieChart pieChart = mActivityRule.getActivity().findViewById(R.id.batteryPieChart);

        mActivityRule.getActivity().updateBatteryRemaining(50);
        assertTrue(pieChart.getCenterText().equals("50%"));
        mActivityRule.getActivity().updateBatteryRemaining(20);
        assertTrue(pieChart.getCenterText().equals("20%"));
    }

    @Test
    public void testDeviceSettingsGrid() {
/*
        try {
            java.lang.reflect.Field field = mActivityRule.getActivity().getClass().getDeclaredField("mOWConnected");
            field.setAccessible(true);
            field.set(mActivityRule.getActivity(), true);
        } catch (Exception e) { }
 */
        final MultiStateToggleButton toggleButton  =
                mActivityRule.getActivity().findViewById(R.id.mstb_multi_ridemodes);
        //mActivityRule.getActivity().updateRideMode(1);
        //assertTrue(toggleButton.getValue() == 1);
        //mActivityRule.getActivity().updateRideMode(2);

    }

    @Test
    public void unsignedShortValidator_ReturnsTrue() {
        int x = Util.unsignedShort(new byte[] { (byte) 0x65, (byte)0x10, (byte)0xf3, (byte)0x29});
        assertTrue(x == 25872);
    }


}

