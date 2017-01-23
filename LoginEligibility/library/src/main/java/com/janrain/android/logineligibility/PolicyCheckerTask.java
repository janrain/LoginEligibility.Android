package com.janrain.android.logineligibility;

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

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PolicyCheckerTask extends AsyncTask<Void, Void, JSONObject> {

    protected static final String LOG_TAG;

    static {
        LOG_TAG = "PolicyCheckerTask";
    }

    public interface PolicyCheckerTaskCompleted {
        void onPolicyCheckerTaskCompleted(JSONObject response);
    }

    private PolicyCheckerTaskCompleted taskCompleted;
    private PolicyCheckerTaskParams taskParams;

    public PolicyCheckerTask() {}

    public void init(PolicyCheckerTaskParams taskParams, PolicyCheckerTaskCompleted taskCompleted) {
        this.taskCompleted = taskCompleted;
        this.taskParams = taskParams;
    }

    @Override
    protected JSONObject doInBackground(Void... params) {

        JSONObject dataToSend = new JSONObject();
        JSONObject policyCheckerData = new JSONObject();
        PolicyCheckerTaskParams taskParams = this.taskParams;
        HttpURLConnection urlConnection = null;
        try {
            JSONObject resourceKey = new JSONObject();
            resourceKey.put("clientId", taskParams.leConfig.captureClientId );

            dataToSend.put("subject", taskParams.subjectKey);
            dataToSend.put("action", "access");
            dataToSend.put("resource", resourceKey);

            String[] pathFields = {
                    taskParams.leConfig.policyCheckerHost,
                    taskParams.leConfig.policyCheckerStage,
                    "tenants",
                    taskParams.leConfig.policyCheckerTenant,
                    "authz_request"};

            String fullUrl = android.text.TextUtils.join("/",pathFields);

            URL url = new URL(fullUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, urlConnection.toString());
            StringBuilder result = new StringBuilder();

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setUseCaches (false);
            urlConnection.setRequestProperty("Content-Type","application/json");
            urlConnection.connect();
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(dataToSend.toString().getBytes());
            out.flush ();
            out.close ();

            InputStream in;
            if(urlConnection.getResponseCode() / 100 == 2){
                in = new BufferedInputStream(urlConnection.getInputStream());
            }else{
                in = new BufferedInputStream(urlConnection.getErrorStream());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();

            policyCheckerData = new JSONObject(result.toString());

        } catch(MalformedURLException ex){
            Log.e(LOG_TAG, ex.getStackTrace().toString());
            throw new LEServiceException(String.format("Malformed URl Exception: %s",ex.getLocalizedMessage()));
        } catch (IOException ex) {
            Log.e(LOG_TAG, ex.getStackTrace().toString());
            throw new LEServiceException(String.format("IO Exception: %s",ex.getLocalizedMessage()));
        } catch(JSONException ex){
            Log.e(LOG_TAG, "Error Creating PolicyChecker JSON");
            throw new LEServiceException("Error Creating PolicyChecker JSON");
        }
        finally {
            if(urlConnection != null)
                urlConnection.disconnect();
        }

        return policyCheckerData;
    }


    @Override
    protected void onPostExecute(JSONObject result) {
        super.onPostExecute(result);

        // In onPostExecute we check if the listener is valid
        if(this.taskCompleted != null) {

            // And if it is we call the callback function on it.
            this.taskCompleted.onPolicyCheckerTaskCompleted(result);
        }
    }
}


