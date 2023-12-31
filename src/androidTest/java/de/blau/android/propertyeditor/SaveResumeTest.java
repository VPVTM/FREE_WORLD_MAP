package de.blau.android.propertyeditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.os.RemoteException;
import android.view.KeyEvent;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.osm.Node;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

/**
 * 1st attempts at testing lifecycle related aspects in easyedit modes
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveResumeTest {

    Context                 context      = null;
    AdvancedPrefDatabase    prefDB       = null;
    Main                    main         = null;
    UiDevice                device       = null;
    Map                     map          = null;
    Logic                   logic        = null;
    private Instrumentation instrumentation;
    ActivityScenario<Main>  mainScenario = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        mainScenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
        mainScenario.moveToState(State.DESTROYED);
    }

    /**
     * Edit a tag in the PropertyEditorActivity - restart app - check that edit is still there
     */
    @Test
    public void editTag() {
        editTag(false);
    }

    /**
     * Edit a tag in the PropertyEditorActivity - rotate - restart app - check that edit is still there
     */
    @Test
    public void editTagRotate() {
        editTag(true);
    }

    /**
     * @param rotate if true try to rotate the device while paused
     */
    private void editTag(boolean rotate) {
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        PropertyEditorData[] single = new PropertyEditorData[1];
        single[0] = new PropertyEditorData(n, null);

        try (ActivityScenario<PropertyEditorActivity> scenario = ActivityScenario
                .launch(PropertyEditorActivity.buildIntent(main, single, false, false, null, null))) {
            Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            assertTrue(propertyEditor instanceof PropertyEditorActivity);
            TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
            final String original = "Bergdietikon";
            UiObject2 o = device.wait(Until.findObject(By.clickable(true).textStartsWith(original)), 500);
            assertNotNull(o);
            final String edited = "dietikonBerg";
            UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith(original));
            try {
                editText.click();
                editText.setText(edited);
                instrumentation.sendCharacterSync(KeyEvent.KEYCODE_TAB); // this seems to be necessary to guarantee
                                                                         // things will be changed
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }

            if (rotate) {
                scenario.moveToState(State.CREATED);
                try {
                    device.unfreezeRotation();
                    device.setOrientationRight();
                    TestUtils.sleep(2000);
                } catch (RemoteException e) {
                    fail(e.getMessage());
                }
                scenario.moveToState(State.RESUMED);
                TestUtils.sleep(2000);
            } else {
                scenario.recreate();
            }

            // check that we still have the change
            o = device.wait(Until.findObject(By.clickable(true).textStartsWith(edited)), 1000);
            assertNotNull(o);
        }
    }
}
