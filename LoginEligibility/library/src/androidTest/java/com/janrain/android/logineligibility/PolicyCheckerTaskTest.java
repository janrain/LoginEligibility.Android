package com.janrain.android.logineligibility;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Copyright (c) 2017, Janrain, Inc.

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.
 * Neither the name of the Janrain, Inc. nor the names of its
 contributors may be used to endorse or promote products derived from this
 software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

@RunWith(AndroidJUnit4.class)
public class PolicyCheckerTaskTest  {

    private LEServiceConfiguration goodConfig;
    private CountDownLatch signal = null;
    private JSONObject testResult;
    private MockWebServer server;
    private PolicyCheckerTask.PolicyCheckerTaskCompleted taskCompleted;
    private PolicyCheckerTask task;
    private String baseUrl;

    @Before
    public void setUp() throws Exception {
        signal = new CountDownLatch(1);
        server = new MockWebServer();
        server.start(30000);
        baseUrl = "http://" + server.getHostName() + ":" + server.getPort();
        goodConfig = new LEServiceConfiguration();
        goodConfig.captureApplicationId = "somecaptureappid";
        goodConfig.captureClientId = "somecaptureclientid";
        goodConfig.captureFlowName = "someflowname";
        goodConfig.captureFlowLocale = "some-locale";
        goodConfig.captureFlowVersion = "some-version";
        goodConfig.policyCheckerStage = "dev";
        goodConfig.policyCheckerTenant = "some-tenant";
        goodConfig.policyCheckerHost = baseUrl;
        task = new PolicyCheckerTask();
        taskCompleted = new PolicyCheckerTask.PolicyCheckerTaskCompleted(){
            @Override
            public void onPolicyCheckerTaskCompleted(JSONObject response){
                testResult = response;
                signal.countDown();
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        signal.countDown();
        server.shutdown();
    }

    @Test
    public void test_policyCheckerTask_violates() throws Exception{

        String violates = "{\"request\":{\"subject\":{\"id\":\"f9fc2109-043b-4e5f-bd0e-f9cbbbc5356d\"},\"resource\":{\"clientId\":\"zwuuekttku9agjg3v8sp5eekk7mvhkq9\"}},\"outcome\":\"Violates\",\"obligations\":[\"UserShouldProvideLongFamilyName\"],\"reasons\":[\"UserIsTooYoung\",\"DisplayNameAbsent\"]}";
        JSONObject subjectKey = new JSONObject();
        try {
            subjectKey.put("accessToken", "abcdefghijk");
        }catch(JSONException jex){}
        server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .addHeader("Cache-Control", "no-cache")
                    .setBody(violates));

        PolicyCheckerTaskParams taskParams = new PolicyCheckerTaskParams(goodConfig, subjectKey);
        task.init(taskParams,taskCompleted);
        task.execute();
        signal.await();
        server.shutdown();
        assertTrue(testResult.has("outcome"));
        assertEquals("Violates", testResult.getString("outcome"));

    }

    @Test
    public void test_policyCheckerTask_complies() throws Exception{

        String complies = "{\"request\":{\"subject\":{\"id\":\"a9629006-fe97-44ad-84de-663f60df1792\"},\"resource\":{\"clientId\":\"zwuuekttku9agjg3v8sp5eekk7mvhkq9\"}},\"outcome\":\"Complies\"}";
        JSONObject subjectKey = new JSONObject();
        try {
            subjectKey.put("accessToken", "abcdefghijk");
        }catch(JSONException jex){}
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(complies));

        PolicyCheckerTaskParams taskParams = new PolicyCheckerTaskParams(goodConfig, subjectKey);
        task.init(taskParams,taskCompleted);
        task.execute();
        signal.await();
        server.shutdown();
        assertTrue(testResult.has("outcome"));
        assertEquals("Complies", testResult.getString("outcome"));

    }

    @Test
    public void test_policyCheckerTask_errors() throws Exception{

        String complies = "{\"errorCode\":\"some error info\"}";
        JSONObject subjectKey = new JSONObject();
        try {
            subjectKey.put("accessToken", "abcdefghijk");
        }catch(JSONException jex){}
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(complies));

        PolicyCheckerTaskParams taskParams = new PolicyCheckerTaskParams(goodConfig, subjectKey);
        task.init(taskParams,taskCompleted);
        task.execute();
        signal.await();
        server.shutdown();
        assertFalse(testResult.has("outcome"));
        assertTrue(testResult.has("errorCode"));
        assertEquals("some error info", testResult.getString("errorCode"));

    }

    @Test
    public void test_policyCheckerTask_empty() throws Exception{

        JSONObject subjectKey = new JSONObject();
        try {
            subjectKey.put("accessToken", "abcdefghijk");
        }catch(JSONException jex){}
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Cache-Control", "no-cache")
                .setBody(""));

        PolicyCheckerTaskParams taskParams = new PolicyCheckerTaskParams(goodConfig, subjectKey);
        task.init(taskParams,taskCompleted);
        task.execute();
        signal.await();
        server.shutdown();
        assertFalse(testResult.has("outcome"));
        assertTrue(testResult.has("errorCode"));
        assertEquals("Null or Empty Response from Policy Checker Server", testResult.getString("errorCode"));

    }
}
