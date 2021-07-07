package owt.test.p2p.apitest;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import owt.p2p.P2PClient;
import owt.test.util.LockServer;


@RunWith(AndroidJUnit4.class)
public class TestDevice1 {
    private static LockServer mLockServer = new LockServer();
    private static String taskId = null;
    private int caseSequenceNumber = 0;

    @Test
    public void testConnect() {
        mLockServer.notifyWorkflowLock("user1", "connect");
        mLockServer.waitWorkflowLock("user2", "connect");
    }

    @BeforeClass
    public static void beforeClass() {
        taskId = InstrumentationRegistry.getArguments().getString("taskId");
        mLockServer.connect(taskId,"role1");
        mLockServer.waitControlLock("task-start");
    }

    @AfterClass
    public static void afterClass() {
        mLockServer.disconnect();
    }

    @After
    public void tearDown() {
        JSONObject lockData = new JSONObject();
        try {
            lockData.put("sequenceNumber", this.caseSequenceNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLockServer.notifyControlLock("case-end", lockData);
        mLockServer.initStatus();
    }

    @Before
    public void setUp() {
        this.caseSequenceNumber++;
        JSONObject lockData = new JSONObject();
        try {
            lockData.put("sequenceNumber", this.caseSequenceNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLockServer.notifyControlLock("case-ready", lockData);
        mLockServer.waitControlLock("case-start");
    }
}
