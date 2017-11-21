package net.kwatts.powtools;


import android.app.Application;
import android.test.ApplicationTestCase;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.regex.Pattern;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import android.content.SharedPreferences;

import com.github.mikephil.charting.charts.PieChart;
import com.rey.material.widget.Button;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;



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
        final org.honorato.multistatetogglebutton.MultiStateToggleButton toggleButton  =
                mActivityRule.getActivity().findViewById(R.id.mstb_multi_ridemodes);
        //mActivityRule.getActivity().updateRideMode(1);
        //assertTrue(toggleButton.getValue() == 1);
        //mActivityRule.getActivity().updateRideMode(2);

    }

    @Test
    public void unsignedShortValidator_ReturnsTrue() {
        int x = MainActivity.unsignedShort(new byte[] { (byte) 0x65, (byte)0x10, (byte)0xf3, (byte)0x29});
        assertTrue(x == 25872);
    }


}

